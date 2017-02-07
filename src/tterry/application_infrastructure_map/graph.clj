(ns tterry.application-infrastructure-map.graph
  (:require [dorothy.core :as d]))

(defn create-application [icon-base-path name dependencies]
  (let [node-name (keyword name)]
    (merge {:application (d/subgraph node-name
                                     [[:node {:label (str "\n\n\n\n" name) :shape :box
                                              :color :white :fontcolor :black
                                              :image (str icon-base-path "Compute/Compute_AmazonEC2.png")}]
                                      node-name])}
           (when (and dependencies (not (empty? dependencies)))
             {:dependencies (mapv (fn [d] [node-name :> d {:dir :forward}]) dependencies)}))))



(defn create-dependency [icon-base-path name dep-type]
   (d/subgraph (keyword name)
              [[:node
                (merge {:label (str "\n\n\n\n" name) :color :white
                        :fontcolor :black :shape :box}
                       (cond (= dep-type :sqs-queues)
                             {:image (str icon-base-path "Messaging/Messaging_AmazonSQS.png")}
                             (= dep-type :kinesis-streams)
                             {:image (str icon-base-path "Analytics/Analytics_AmazonKinesis_AmazonKinesisStreams.png")}
                             (= dep-type :s3-buckets)
                             {:image (str icon-base-path "Storage/Storage_AmazonS3_bucket.png")}
                             (= dep-type :route53-records)
                             {:image (str icon-base-path "Networking & Content Delivery/NetworkingContentDelivery_AmazonRoute53.png")}
                             (= dep-type :jdbc)
                             {:image (str icon-base-path "Database/Database_AmazonRDS.png")}
                             (= dep-type :elastic-cache)
                             {:image (str icon-base-path "Database/Database_AmazonElasticCache.png")}))]
               (keyword name)]))