(ns tterry.application-infrastructure-map.core
  (:require [dorothy.core :as d]
            [tterry.application-infrastructure-map.graph :as g]
            [tterry.application-infrastructure-map.application :as a]))

(defn apps-dependencies [apps dependency-type]
  ;find the dependencys of specified type for all apps
  (apply concat (map #(dependency-type %) apps)))

(defn create-dependencies [icon-base-path applications]
  ;for each application find all dependency types
  (let [dependency-types [:s3-buckets :kinesis-streams :route53-records :jdbc :sqs-queues :elastic-cache]]
    (->> (map (fn [dt]
                (map #(g/create-dependency icon-base-path % dt) (apps-dependencies applications dt)))
              dependency-types)
         (apply concat))))

(defn create-infrastructure [icon-base-path applications]
  ;create the infrastructure graph.
  (println "Creating infrastructure graph...")
  (let [dependencies (create-dependencies icon-base-path applications)
        app-graphs-with-deps (mapv (fn[app] (g/create-application icon-base-path (:name app) (:all-dependencies app))) applications)
        app-graphs (mapv #(:application %) app-graphs-with-deps)
        deps-graph (apply concat (mapv #(:dependencies %) app-graphs-with-deps))]
    (concat [] dependencies app-graphs deps-graph)))

(defn create
  [image-output-file dot-output-file app-definitions dependency-filters icon-base-path]
   (try
     (let [applications (map (fn[app-def]
                               (println (str "Parsing application " (key app-def)))
                               (a/parse-app (key app-def) (val app-def) dependency-filters)
                               ) app-definitions)
           infrastructor (create-infrastructure icon-base-path applications)
           dot-str (-> (d/graph infrastructor) d/dot)]
       (spit dot-output-file dot-str)
       (println (str "Wrote dot file to " (.getAbsolutePath dot-output-file)))
       (d/save! dot-str image-output-file {:format :png :layout "fdp"})
       (println (str "Wrote graph image to " (.getAbsolutePath image-output-file)))
       (d/show! dot-str {:layout "fdp"}))
     (catch Exception e
       (clojure.stacktrace/print-stack-trace e))))

