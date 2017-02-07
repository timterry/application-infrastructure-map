(ns tterry.application-infrastructure-map.application
  (:require [clojure.set :as cset]
            [tterry.application-infrastructure-map.nomad-config-parser :as ncp]
            [tterry.application-infrastructure-map.lein-project-parser :as lpp]))
(defn walk-map
  ([m]
   (if (map? m)
     (walk-map (mapv (fn[k] [k]) (keys m)) m {})
     m))
  ([map-keys m result]
   (let [key (first map-keys)
         value (get-in m key)
         remaining-keys (rest map-keys)]
     (if (map? value)
       (let [next-level-keys (mapv (fn [k] (conj key k)) (keys value))]
         (recur (concat next-level-keys remaining-keys) m result))
       (let [new-result {key value}
             new-results (conj result new-result)]
         (if-not (empty? remaining-keys)
           (recur remaining-keys m new-results)
           new-results))))))

(defn filter-for-names [names app]
  (let [filtered-names (filter
                         (fn[entry]
                           (some #{(second entry)} names))
                         (:config map))]
    (-> (mapv val filtered-names)
        distinct)))

(defn filter-for-regex
  ([regex app]
   (filter-for-regex regex second app))
  ([regex result-fn map]
   (let [filtered-names (filter
                          (fn[entry]
                            (re-matches regex (str (second entry))))
                          (:config map))]
     (-> (mapv (fn[n] (->> (val n)
                       (re-matches regex)
                       result-fn)) filtered-names)
         distinct))))

(defn filter-for-name-regex [regex app]
  (when-let [matched-name (re-matches regex (:name app))]
    [(second matched-name)]))

(defn filter-for-jdbc [app]
  (->> (filter-for-regex
         #"^jdbc:postgresql://([^:]+)(:[0-9]+){0,1}/(.+)"
         (fn[n]
           (str (second n) " " (nth n 3)))
         app)
       (mapv (fn[n] (.replaceAll n "/" " ")))))

(defn filter-for-regex-with-blacklist [regex blacklist app]
  (-> (filter-for-regex regex app)
      set
      (cset/difference (set blacklist))
      vec))

(defn filter-for-dependencies-regex [regex app]
  (when-let [deps (:dependencies app)]
    (->> (map (fn[d] (-> (re-matches regex d)
                        second)) deps)
        (remove nil?))))

(defn parse-app [app-name {:keys [nomad-config lein-project] :as app-def} dependency-filters]
  (let [app-config (ncp/read-nomad-config nomad-config)
        app-deps (when lein-project (lpp/read-lein-project-deps lein-project))
        flattened-config (walk-map app-config)
        parsed-app (merge {:name app-name
                           :config flattened-config}
                          (when app-deps
                            {:dependencies app-deps}))
        deps (->> (map (fn[filter]
                         (let [f (val filter)]
                           (when-let [filtered-deps (f parsed-app)]
                             {(key filter) filtered-deps}))
                         ) dependency-filters)
                  (apply merge))]
    (merge {:name app-name
            :all-dependencies (apply concat (vals deps))}
           deps)))

