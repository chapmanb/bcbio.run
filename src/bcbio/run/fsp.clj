(ns bcbio.run.fsp
  "File naming and manipulation commands that supplement the fs library (fsplus)."
  (:import [java.io File])
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

;; ## Naming
;; Generate new file names from existing ones

(defn split-ext+
  "Split extension, grouping zipped extensions with rest of file"
  [f]
  (let [dir (let [d (str (fs/parent f))]
              (if (.startsWith f d) (str d "/") ""))
        [base-o ext] (fs/split-ext f)
        [base ext2] (if (contains? #{".gz" ".bz2" ".zip"} ext)
                      (fs/split-ext base-o)
                      [base-o ""])]
    [(str dir base) (str ext2 ext)]))

(defn file-root
  "Retrieve file name without extension: /path/to/fname.txt -> /path/to/fname"
  [fname]
  (first (split-ext+ fname)))

(defn add-file-part
  "Add file extender: base.txt -> base-part.txt"
  ([fname part]
     (add-file-part fname part nil))
  ([fname part out-dir]
     (add-file-part fname part out-dir nil))
  ([fname part out-dir new-ext]
     (let [[base ext] (split-ext+ fname)
           out-fname (format "%s-%s%s" base part (if new-ext new-ext ext))]
       (if-not (nil? out-dir)
         (str (io/file out-dir (fs/base-name out-fname)))
         out-fname))))

(defn remove-file-part
  "Remove file specialization extender: base-part.txt -> base.txt"
  [fname part]
  (string/replace (str fname) (str "-" part) ""))

(defn remove-zip-ext
  "Remove any zip extensions from the input filename"
  [fname]
  (letfn [(maybe-remove-ext [fname ext]
            (if (.endsWith fname ext)
              (subs fname 0 (- (.length fname) (.length ext)))
              fname))]
    (let [exts [".tar.gz" "tar.bz2" ".gz" ".bz2" ".zip"]]
      (reduce maybe-remove-ext fname exts))))

;; ## File and directory manipulation

(defn remove-path
  "Remove file or directory only if it exists."
  [x]
  (if (fs/exists? x)
    (if (fs/directory? x)
      (fs/delete-dir x)
      (fs/delete x))))

(defn abspath
  "Produce a normalized file path, expanding home directories."
  [f]
  (-> (io/file f)
      fs/expand-home
      fs/absolute-path
      str))

(defn safe-mkdir
  "Make a directory, returning the created directory name."
  [d]
  (fs/mkdirs d)
  (str d))
