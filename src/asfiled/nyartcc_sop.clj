(ns ^{:author "Daniel Leong"
      :doc "SOPs for nyartcc"}
  asfiled.nyartcc-sop
  (:require [clojure.string :refer [upper-case]]))

(def sop-runways-kjfk
  [{:when {:speed [0 4] :dir [[0 360]]}
    :use "JFK: Depart: 31L/R  Land: 31L/R"
    :tags [:jfk-depart-31 :jfk-land-31]}])

(def sop-klga
  {:exit-intervals
   {:north [[290 360] [0 15]]
    :east [[16 95]]
    :south [[96 215]]
    :west [[216 289]]}
   :exit-gates
   {:north ["GAYEL" "HAAYS" "NEION" "COATE" "SAX" "NYACK"]
    :east ["GREKI" "MERIT" "BAYYS" "CMK" "BDR"]
    :south ["SHIPP" "WAVEY" "DIXIE" "WHITE" "DPK"]
    :west ["NEWEL" "ELIOT" "ZIMMZ!" "PARKE" "LANNA" "BIGGY" "SBJ"]}
   :runway-selection
   (concat
     [{:when {:speed [0 4] :dir [[0 360]]}
       :use "LGA: Depart: 31  Land: VMC 22, IMC ILS22"
       :tags [:lga-depart-31 :lga-land-22]}
      {:when {:speed [5 14] :dir [[315 360] [0 44]]}
       :use "LGA: Depart: 4  Land: VMC EXP31, IMC LOC31"
       :tags (:lga-depart-4 :lga-land-31)}
      ]
     ;; merge with JFK to get its tags
     sop-runways-kjfk)
   :sid-selection
   [{:when [:lga-depart-13 {:not [:lga-land-22]}
            :jfk-depart-13 :jfk-land-ils13]
     :type :jets
     :gates :any
     :climb "FLUSHING"
     :rnav "TNNIS#"}]
   })
;; NB: For runway selection, we examine ALL options
;;  and evaluate ALL matches. This means we merge
;;  the tags and merge the :use strings, in the
;;  order they were accepted. The :use strings
;;  are joined with a newline
;; For SID selection, each item in the :when array
;;  is tested against the set of tags. Items in this
;;  array may be:
;;   - A single symbol/string: Exact match (convenience)
;;   - {:not [symbols]}: Anything BUT the symbols
;;   - {:any [symbols]}: ANY of the symbols
;;  Then, ALL matching configurations are returned.

(def sop-map
  {"KLGA" sop-klga})

(defn get-sop [facility-icao]
  (get sop-map (upper-case facility-icao)))
