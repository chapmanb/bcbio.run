(ns bcbio.run.test-itx
  "Test code for idempotent file running"
  (:use [midje.sweet])
  (:require [me.raynes.fs :as fs]
            [bcbio.run.fsp :as fsp]
            [bcbio.run.itx :as itx]))

(fact "Idempotent processing of files"
  (itx/needs-run? "project.clj") => false
  (itx/needs-run? "noexist.txt") => true
  (itx/needs-run? "project.clj" "README.md") => false
  (itx/needs-run? ["project.clj" "README.md"]) => false
  (itx/needs-run? "project.clj" "noexist.txt") => true)

(let [kwds {:test "new"}]
  (fact "Allow special keywords in argument paths."
    (itx/subs-kw-files [:test] kwds) => ["new"]
    (itx/subs-kw-files [:test "stay"] kwds) => ["new" "stay"]
    (itx/subs-kw-files [:nope "stay"] kwds) => [:nope "stay"]))

(facts "Manipulating file paths"
  (fsp/add-file-part "test.txt" "add") => "test-add.txt"
  (fsp/add-file-part "/full/test.txt" "new") => "/full/test-new.txt"
  (fsp/file-root "/full/test.txt") => "/full/test"
  (fsp/remove-zip-ext "test.txt") => "test.txt"
  (fsp/remove-zip-ext "test.txt.gz") => "test.txt"
  (fsp/remove-zip-ext "test.tar.gz") => "test")
