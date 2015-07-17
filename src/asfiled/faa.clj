(ns ^{:author "Daniel Leong"
      :doc "Utility methods using faa.gov"}
  asfiled.faa
  (:require [clojure.string :refer [upper-case]]
            [org.httpkit.client :as http] 
            [hickory
             [core :refer [parse parse-fragment as-hickory]]
             [select :as s]]))

(defn- clean-icao 
  "US airports must NOT include the K"
  [raw]
  (let [base (upper-case raw)] 
    (cond 
      (= 3 (count base)) base
      (= \K (first base)) (-> base (.substring 1))
      :else base)))

(def url-prd "http://www.fly.faa.gov/rmt/d_prefroutes.jsp")

(defn to-route-row
  [hickory-tr]
  (->> hickory-tr
       (s/select (s/descendant
                   (s/tag :td)
                   s/last-child
                   (s/not s/element))) ;; select all the text nodes
       (zipmap [:from :to :type :num :hour1 :hour2 :hour3
                :area :aircraft :altitude :direction :route
                :from-artcc :to-artcc])))

(defn load-preferred-routes
  [from to]
  (let [f (clean-icao from)
        t (clean-icao to)]
    (let [options {:form-params {:Origin f :Destin t
                                 :Type "" :Area "" :Aircraft ""
                                 :Altitude "" :Route "" :Direction ""
                                 :DCenter "" :ACenter ""
                                 :search_database "Submit Search Terms"}}
          {:keys [err body]} @(http/post url-prd options)]
      (if err
        nil
        (->> body
             parse 
             as-hickory
             (s/select (s/descendant
                         (s/class :mainArea)
                         (s/tag :table)
                         (s/tag :table) ;; (sic)
                         (s/tag :tr)))
             rest
             (map to-route-row))))))
