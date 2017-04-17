(defproject tterry/application-infrastructure-map "0.1.0-SNAPSHOT"
  :description "Creates a graph of application infrastructure by reading nomad config and lein project.clj. Outputs to .png and graphviz .dot file"
  :url "https://github.com/timterry/application-infrastructure-map"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [dorothy "0.0.6"]
                 [amazonica "0.3.85"]
                 [jarohen/nomad "0.7.0"]
                 [org.clojure/tools.reader "1.0.0-beta4"]])
