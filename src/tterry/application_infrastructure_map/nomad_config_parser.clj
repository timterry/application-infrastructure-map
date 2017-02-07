(ns tterry.application-infrastructure-map.nomad-config-parser
  (:require [nomad :as nomad]
            [clojure.tools.reader.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :refer [postwalk-replace]])
  (:import (java.io File)))

(defn- replace-nomad-nils [m]
  (postwalk-replace {:nomad/nil nil} m))

(defn- nomad-data-readers [snippet-reader]
  {'nomad/file io/file
   'nomad/snippet snippet-reader
   'nomad/env-var #(or (System/getenv %) :nomad/nil)
   ;'nomad/edn-env-var read-edn-env-var
   })

(defn- readers-without-snippets []
  {:readers (nomad-data-readers (constantly ::snippet))})

(defn- readers-with-snippets [snippets]
  {:readers (nomad-data-readers
              (fn [ks]
                (or
                  (get-in snippets ks)
                  (throw (ex-info "No snippet found for keys" {:keys ks})))))})

(defn read-nomad-config [filename]
  (let [config-str (slurp (File. filename))
        without-snippets (edn/read-string (readers-without-snippets) config-str)
        snippets (get without-snippets :nomad/snippets)
        with-snippets (-> (edn/read-string (readers-with-snippets snippets)
                                           config-str)
                          (dissoc :nomad/snippets)
                          replace-nomad-nils)]
    (or (get-in with-snippets [:nomad/environments "prod"])
        (get-in with-snippets [:nomad/environments :prod]))))
