(ns tterry.application-infrastructure-map.lein-project-parser)

;https://github.com/clojure-cookbook/clojure-cookbook/blob/master/04_local-io/4-14_read-write-clojure-data-structures.asciidoc
(defn- read-one
  [r]
  (try
    (read r)
    (catch java.lang.RuntimeException e
      (if (= "EOF while reading" (.getMessage e))
        ::EOF
        (throw e)))))

(defn read-seq-from-file
  "Reads a sequence of top-level objects in file at path."
  [path]
  (with-open [r (java.io.PushbackReader. (clojure.java.io/reader path))]
    (binding [*read-eval* false]
      (doall (take-while #(not= ::EOF %) (repeatedly #(read-one r)))))))

(defn filter-project-def [clj]
  (= "defproject" (-> clj first name)))

(defn read-lein-project-deps [filename]
  (let [project-seq (read-seq-from-file filename)
        filtered-seq (-> (filter filter-project-def project-seq)
                         first)
        deps (->> (map-indexed (fn[i item]
                            (when (= :dependencies item)
                              (nth filtered-seq (inc i))))
                          filtered-seq)
                 (remove nil?)
                  first)
        dep-names (map (fn[d] (-> d first name)) deps)]
    dep-names))
