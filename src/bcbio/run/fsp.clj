(ns bcbio.run.fsp
  "File naming and manipulation commands that supplement the fs library (fsplus)."
  (:import [java.io File])
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

;; ## Naming
;; Generate new file names from existing ones

(defn file-root
  "Retrieve file name without extension: /path/to/fname.txt -> /path/to/fname"
  [fname]
  (let [i (.lastIndexOf fname ".")]
    (if (pos? i)
      (subs fname 0 i)
      fname)))

(defn add-file-part
  "Add file extender: base.txt -> base-part.txt"
  ([fname part]
     (add-file-part fname part nil))
  ([fname part out-dir]
     (let [out-fname (format "%s-%s%s" (file-root fname) part (fs/extension fname))]
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
