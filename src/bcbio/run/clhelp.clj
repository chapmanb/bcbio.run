(ns bcbio.run.clhelp
  "Helper functionality for writing command lines that deal with biological file formats."
  (:require [bcbio.run.fsp :as fsp]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(defn- is-vcf?
  [f]
  (with-open [rdr (fsp/pog-reader f)]
    (.startsWith (first (line-seq rdr)) "##fileformat=VCF")))

(defn- is-bam?
  "Handle CRAM and BAM compressed inputs."
  [f]
  (or (.endsWith f ".bam")
      (.endsWith f ".cram")))

(defn- get-ftype
  [f]
  (cond
   (is-bam? f) :bam
   (is-vcf? f) :vcf
   :else :list))

(defn- exists-or-gz?
  [f]
  (or (and (fs/exists? f) (fs/file? f))
      (and (fs/exists? (str f ".gz")) (fs/file? (str f ".gz")))))

(defn- get-vcf-bam-flex
  "Handle retrieving VCF and BAM files from either a single file or text-based list of files."
  [f]
  (if (not (exists-or-gz? f))
    [[:missing f]]
    (let [ftype (get-ftype f)]
      (if (= :list ftype)
        (with-open [rdr (io/reader f)]
           (->> (line-seq rdr)
                (map string/trimr)
                (remove empty?)
                (mapcat get-vcf-bam-flex)
                vec))
        [[ftype f]]))))

(defn vcf-bam-args
  "Retrieve VCF, BAM and CRAM files from supplied command line arguments.
   Returns a map of file types with available and missing."
  [xs]
  (reduce (fn [coll [ftype f]]
            (assoc coll ftype (conj (get coll ftype []) f)))
          {} (mapcat get-vcf-bam-flex xs)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn check-missing
  "Report messages on required missing command line arguments"
  [options required]
  (map (fn [opt] (format "Missing required option: %s" (name opt)))
       (remove #(contains? options %) required)))
