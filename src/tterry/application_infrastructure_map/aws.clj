(ns tterry.application-infrastructure-map.aws
  (:require [amazonica.aws.sqs :as sqs]
            [amazonica.aws.sns :as sns]
            [amazonica.aws.kinesis :as kinesis]
            [amazonica.aws.s3 :as s3]
            [again.core :as again]))

(def sqs-endpoint "sqs.eu-west-1.amazonaws.com")
(def sns-endpoint "sns.eu-west-1.amazonaws.com")
(def kinesis-endpoint "kinesis.eu-west-1.amazonaws.com")
(def s3-endpoint "s3-eu-west-1.amazonaws.com")

(def backoff-delays [1000 1500 2500])

(defn paged-function
  ([func results-key]
   (let [results (again/with-retries backoff-delays
                                     (func))]
     (if-let [next-token (:next-token results)]
       (paged-function func results-key next-token (get results results-key))
       (do
         (get results results-key)))))
  ([func results-key next-token previous-results]
   (let [results (again/with-retries backoff-delays
                                     (func {:next-token next-token}))
         asgs (concat previous-results (get results results-key))]
     (if-let [next-token (:next-token results)]
       (recur func results-key next-token asgs)
       (do
         asgs)))))

(defn sqs-queues []
  (let [queue-urls (paged-function
                     (fn[& args]
                       (sqs/list-queues {:endpoint sqs-endpoint} args))
                     :queue-urls)]
    (mapv (fn[q]
            (->> q (re-matches #"https://sqs.eu-west-1.amazonaws.com/[0-9]+/(.+)")
                 second))
            queue-urls)))

(defn sns-topics []
  (let [topic-arns (:topics
                     (sns/list-topics {:endpoint sns-endpoint}))]
    (mapv (fn[t]
           (->> (:topic-arn t)
                (re-matches #"arn\\:aws:eu-west-1\\:[0-9]+:\\(.+)")
                second)
           ) topic-arns)))

(defn sns-topic-subscriptions [sns-topics]

  )

(defn kinesis-streams []
  (let [stream-names (:stream-names
                       (kinesis/list-streams {:endpoint kinesis-endpoint}))]
    stream-names))

(defn s3-buckets []
  (let [buckets (s3/list-buckets {:endpoint s3-endpoint})]
    (mapv (fn[b] (:name b)) buckets)))
