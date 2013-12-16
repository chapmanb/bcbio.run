(ns bcbio.run.itx
   "Functionality for running idempotent, transactional processes.
   Provides an API for long running processes in computational
   pipelines. Avoids re-running a process if it has produced the
   output file on a previous run, and leaving partially finished
   files in the case of premature termination."
  (:import [java.io File])
  (:require [amalloy.ring-buffer :refer [ring-buffer]]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.core.strint :refer [<<]]
            [me.raynes.fs :as fs]
            [taoensso.timbre :as timbre]))

;; ## Idempotent processing
;; avoid re-running when output files exist

(defn needs-run?
  "Check if an output files need a run: any do not exist or empty file"
  [& fnames]
  (letfn [(file-non-empty? [f]
            (and (fs/exists? f)
                 (> (fs/size f) 0)))]
    (not-every? true?
                (map file-non-empty? (flatten fnames)))))

(defn up-to-date?
  "Ensure a derived file is up to date with the parent file."
  [derived parent]
  (>= (fs/mod-time derived) (fs/mod-time parent)))

(defn subs-kw-files
  "Substitute any keywords in the arguments from file information map."
  [args file-info]
  (letfn [(maybe-sub-kw [x]
            (if (and (keyword? x)
                     (contains? file-info x))
              (get file-info x)
              x))]
    (map maybe-sub-kw args)))

;; ## Transactions
;; Handle output files in a separate transaction directory to avoid
;; partially finished output files if long-running processes fail.

(defn temp-dir-w-prefix [root prefix]
  (let [dir (File/createTempFile prefix "" (io/file root))]
    (fs/delete dir)
    (fs/mkdir dir)
    dir))

(defmacro with-temp-dir
  "Provide a temporary directory, removed when exiting the body."
  [[tmp-dir base-dir] & body]
  `(let [~tmp-dir (temp-dir-w-prefix ~base-dir "tmp")]
     (try
       ~@body
       (finally
        (fs/delete-dir ~tmp-dir)))))

(defn safe-tx-files
  "Update file-info with need-tx files in a safe transaction directory."
  [file-info need-tx]
  (let [tx-files (map #(get file-info %) need-tx)
        tx-dir (temp-dir-w-prefix (fs/parent (first tx-files)) "txtmp")]
    (reduce (fn [m [k v]]
              (assoc m k v))
            file-info
            (zipmap need-tx
                    (map #(str (fs/file tx-dir (fs/base-name %))) tx-files)))))

(defn rename-tx-files
  "Rename generated transaction files into expected file location."
  [tx-file-info file-info need-tx exts]
  (doseq [tx-key need-tx]
    (let [tx-safe (get tx-file-info tx-key) 
          tx-final (get file-info tx-key)]
      (fs/rename tx-safe tx-final)
      (doseq [ext exts]
        (when (fs/exists? (str tx-safe ext))
          (fs/rename (str tx-safe ext) (str tx-final ext)))))))

(defmacro with-tx-files
  "Perform action with files, keeping need-tx files in a transaction."
  [[tx-file-info file-info need-tx exts] & body]
  (if (= (count need-tx) 0)
    `(do ~@body)
    `(let [~tx-file-info (safe-tx-files ~file-info ~need-tx)]
       (try
         (let [out# (do ~@body)]
           (rename-tx-files ~tx-file-info ~file-info ~need-tx ~exts)
           out#)
         (finally
          (fs/delete-dir (fs/parent (get ~tx-file-info (first ~need-tx)))))))))

(defmacro with-tx-file
  "Handle a single file in a transaction directory."
  [[tx-file orig-file] & body]
  `(let [~tx-file (:out (safe-tx-files {:out ~orig-file} [:out]))]
     (try
       (let [out# (do ~@body)]
         (rename-tx-files {:out ~tx-file} {:out ~orig-file} [:out] [])
         out#)
       (finally
        (fs/delete-dir (fs/parent ~tx-file))))))

;; ## Run external commands

(defn- log-stdout
  "Enable logging of stdout/stderr in real time."
  [proc]
  (let [stdout (->> (.getInputStream proc) java.io.InputStreamReader. java.io.BufferedReader. line-seq)]
    (reduce (fn [acc line]
              (timbre/info line)
              (cons line acc))
            (ring-buffer 100) stdout)))

(defn check-run
  [cmd]
  (timbre/set-config! [:timestamp-pattern] "yyyy-MM-dd HH:mm:ss")
  (let [builder (-> (ProcessBuilder. (into-array String ["bash" "-c" (str "set -o pipefail; " cmd)]))
                    (.redirectErrorStream true))
        proc (.start builder)
        stdout-handle (future (log-stdout proc))
        exit-code (.waitFor proc)]
    (when-not (= 0 exit-code)
      (let [e (Exception. (format "Shell command failed: %s\n%s" cmd (string/join "\n" @stdout-handle)))]
        (timbre/error e)
        (throw e)))))

(defmacro run-cmd
  "Run a command line producing the given output file in an idempotent transaction.
   Wraps all of the machinery around preparing a command line from local arguments.
   Ensures a single run and avoids partial output files."
  [out-file & cmd]
  `(do (when (needs-run? ~out-file)
         (with-tx-file [tx-out-file# ~out-file]
           (let [fill-cmd# (<< ~@cmd)
                 tx-cmd# (string/replace fill-cmd# ~out-file tx-out-file#)]
             (check-run tx-cmd#))))
       ~out-file))
