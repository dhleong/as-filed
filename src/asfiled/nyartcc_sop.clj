(ns ^{:author "Daniel Leong"
      :doc "SOPs for nyartcc"}
  asfiled.nyartcc-sop
  (:require [clojure.string :refer [upper-case join]]))

(def sop-runways-kjfk
  [{:when {:speed [0 4] :dir [[0 360]]}
    :use "JFK: Depart: 31L/R  Land: 31L/R"
    :tags [:jfk-depart-31 :jfk-land-31]}
   ;; > 4KT
   {:when {:speed [5 999] :dir [[0 99]]}
    :use "JFK: Depart: 4L  Land: 4R"
    :tags [:jfk-depart-4 :jfk-land-4]}
   {:when {:speed [5 999] :dir [[100 159]]}
    :use "JFK: Depart: 13L/R  Land: 13L/R"
    :tags [:jfk-depart-13 :jfk-land-13]}
   {:when {:speed [5 999] :dir [[160 259]]}
    :use "JFK: Depart: 22R  Land: 22L"
    :tags [:jfk-depart-22r :jfk-land-22]}
   {:when {:speed [5 999] :dir [[260 359]]}
    :use "JFK: Depart: 31L/R  Land: 31L/R"
    :tags [:jfk-depart-31 :jfk-land-31]}
   ; RVR 1200-1800
   {:when {:speed [0 999] :rvr [1200 1800] :dir [[0 99]]}
    :use "JFK: Depart: 4L  Land: 4R"
    :tags [:jfk-depart-4 :jfk-land-4]}
   {:when {:speed [0 999] :rvr [1200 1800] :dir [[100 159]]}
    :use "JFK: Depart: 13R  Land: 13L"
    :tags [:jfk-depart-13 :jfk-land-13]}
   {:when {:speed [0 999] :rvr [1200 1800] :dir [[160 259]]}
    :use "JFK: Depart: 22R  Land: 22L"
    :tags [:jfk-depart-22r :jfk-land-22]}
   {:when {:speed [0 999] :rvr [1200 1800] :dir [[260 309]]}
    :use "JFK: Depart: 31L  Land: 22L"
    :tags [:jfk-depart-31 :jfk-land-22]}
   {:when {:speed [0 999] :rvr [1200 1800] :dir [[310 359]]}
    :use "JFK: Depart: 31L  Land: 4R"
    :tags [:jfk-depart-31 :jfk-land-4]}
   ; RVR 0-1200
   {:when {:speed [0 999] :rvr [0 1200] :dir [[0 99]]}
    :use "JFK: Depart: 4L  Land: 4R"
    :tags [:jfk-depart-4 :jfk-land-4]}
   {:when {:speed [0 999] :rvr [0 1200] :dir [[100 129]]}
    :use "JFK: Depart: 13R  Land: 4R"
    :tags [:jfk-depart-13 :jfk-land-4]}
   {:when {:speed [0 999] :rvr [0 1200] :dir [[130 159]]}
    :use "JFK: Depart: 13R  Land: 22L"
    :tags [:jfk-depart-13 :jfk-land-22]}
   {:when {:speed [0 999] :rvr [0 1200] :dir [[160 259]]}
    :use "JFK: Depart: 22R  Land: 22L"
    :tags [:jfk-depart-22r :jfk-land-22]}
   {:when {:speed [0 999] :rvr [0 1200] :dir [[260 309]]}
    :use "JFK: Depart: 31L  Land: 22L"
    :tags [:jfk-depart-31 :jfk-land-22]}
   {:when {:speed [0 999] :rvr [0 1200] :dir [[310 359]]}
    :use "JFK: Depart: 31L  Land: 4R"
    :tags [:jfk-depart-31 :jfk-land-4]}
   ])

(def lga-land-catchall
  {:not []})
(def jfk-depart-catchall
  {:any [:jfk-depart-22r :jfk-depart-13 :jfk-depart-4l]})
(def jfk-land-catchall
  {:any [:jfk-land-4 :jfk-land-dme22l :jfk-land-vis22l]})

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
      ; 5-14 KT
      {:when {:speed [5 14] :dir [[315 360] [0 44]]}
       :use "LGA: Depart: 4  Land: VMC EXP31, IMC LOC31"
       :tags [:lga-depart-4 :lga-land-31]}
      {:when {:speed [5 14] :dir [[45 134]]}
       :use "LGA: Depart: 13  Land: VMC 4, IMC ILS4"
       :tags [:lga-depart-13 :lga-land-4]}
      {:when {:speed [5 14] :dir [[135 259]]}
       :use "LGA: Depart: 13  Land: VMC 22, IMC ILS22"
       :tags [:lga-depart-13 :lga-land-22]}
      {:when {:speed [5 14] :dir [[260 314]]}
       :use "LGA: Depart: 31  Land: VMC 22, IMC ILS22"
       :tags [:lga-depart-31 :lga-land-22]}
      ; 15-25 KT
      {:when {:speed [15 25] :dir [[315 360] [0 44]]}
       :use "LGA: Depart: 4  Land: VMC LOC31, IMC LOC31"
       :tags [:lga-depart-4 :lga-land-31]}
      {:when {:speed [15 25] :dir [[45 134]]}
       :use "LGA: Depart: 13  Land: VMC ILS4, IMC ILS4"
       :tags [:lga-depart-13 :lga-land-4]}
      {:when {:speed [15 25] :dir [[135 222]]}
       :use "LGA: Depart: 13  Land: ILS22"
       :tags [:lga-depart-13 :lga-land-22]}
      {:when {:speed [15 25] :dir [[225 314]]}
       :use "LGA: Depart: 31  Land: ILS22"
       :tags [:lga-depart-31 :lga-land-22]}
      ; 25+ KT
      {:when {:speed [25 999] :dir [[0 89]]}
       :use "LGA: Depart: 4  Land: VMC ILS4, IMC ILS4"
       :tags [:lga-depart-4 :lga-land-4]}
      {:when {:speed [25 999] :dir [[90 179]]}
       :use "LGA: Depart: 13  Land: VMC ILS22 CIR13, IMC ILS13"
       :tags [:lga-depart-13 :lga-land-13]}
      {:when {:speed [25 999] :dir [[180 269]]}
       :use "LGA: Depart: 22  Land: ILS22"
       :tags [:lga-depart-22 :lga-land-22]}
      {:when {:speed [25 999] :dir [[270 359]]}
       :use "LGA: Depart: 31  Land: LOC31"
       :tags [:lga-depart-31 :lga-land-31]}
      ; RVR 1200-2400
      {:when {:speed [0 999] :rvr [1200 2400] :dir [[315 360] [0 44]]}
       :use "LGA: Depart: 31  Land: ILS 22"
       :tags [:lga-depart-31 :lga-land-22]}
      {:when {:speed [0 999] :rvr [1200 2400] :dir [[045 134]]}
       :use "LGA: Depart: 13  Land: ILS 22"
       :tags [:lga-depart-13 :lga-land-22]}
      {:when {:speed [0 999] :rvr [1200 2400] :dir [[135 224]]}
       :use "LGA: Depart: 13  Land: ILS22"
       :tags [:lga-depart-13 :lga-land-22]}
      {:when {:speed [0 999] :rvr [1200 2400] :dir [[225 314]]}
       :use "LGA: Depart: 31  Land: ILS22"
       :tags [:lga-depart-31 :lga-land-22]}
      ]
     ;; merge with JFK to get its tags
     sop-runways-kjfk)
   :sid-selection
   [{:when [:lga-depart-13 lga-land-catchall
            :jfk-depart-13 :jfk-land-ils13]
     :use "(All Types) (All Gates) [FLUSHING] [TNNIS#]"}
    {:when [:lga-depart-13 lga-land-catchall
            :jfk-depart-13 :jfk-land-ils22]
     :use "(All Types) (All Gates) [FLUSHING] [TNNIS#]"}
    {:when [:lga-depart-13 lga-land-catchall
            :jfk-depart-13 :jfk-land-vor13]
     :use "(All Types) (All Gates) [WHITESTONE] [TNNIS#]"}
    {:when [:lga-depart-13 lga-land-catchall
            :jfk-depart-31 :jfk-land-ils22]
     :use "(All Types) (All Gates) [FLUSHING] [TNNIS#]"}
    {:when [:lga-depart-13 lga-land-catchall
            :jfk-depart-31 {:not [:jfk-land-ils22]}]
     :use "(All Types) (All Gates) [WHITESTONE] [TNNIS#]"}
    {:when [:lga-depart-13 lga-land-catchall
            :jfk-depart-22r {:any [:jfk-land-ils22 :jfk-land-22]}]
     :use (join "\n"
                ["JETS (South Gates) [CONEY]      [NTHNS#]"
                 "JETS (W/N/E Gates) [MASPETH]    [GLDMN#]"
                 "PROP (All Gates)   [FLUSHING**] [TNNIS#]"])}
    {:when [:lga-depart-13 :lga-land-22
            jfk-depart-catchall jfk-land-catchall]
     :use (join "\n"
                ["JETS (South Gates) [CONEY]      [NTHNS#]"
                 "JETS (W/N   Gates) [MASPETH]    [GLDMN#]"
                 "JETS (East  Gates) [WHITESTONE] [TNNIS#]"
                 "PROP (All Gates)   [WHITESTONE] [TNNIS#]"])}
    {:when [:lga-depart-13 {:not [:lga-land-22]}
            jfk-depart-catchall jfk-land-catchall]
     :use (join "\n"
                ["JETS (South Gates) [CONEY]      [NTHNS#]"
                 "JETS (W/N/E Gates) [WHITESTONE] [TNNIS#]"
                 "PROP (All Gates)   [WHITESTONE] [TNNIS#]"])}
    {:when [:lga-depart-22 :lga-land-22
            :jfk-depart-31 :jfk-land-31]
     :use "(All Types) (All Gates) [As Published] [JUTES#]"}
    {:when [:lga-depart-22 :lga-land-22
            {:not [:jfk-depart-31]} :jfk-land-13]
     :use "(All Types) (All Gates) [As Published] [JUTES#]"}
    {:when [:lga-depart-22 :lga-land-22
            {:not [:jfk-depart-31]} {:any [:jfk-land-4 :jfk-land-22]}]
     :use (join "\n"
                ["ALL  (W/N/E Gates) [As Published] [JUTES#]"
                 "JETS (South Gates) [Follow  RNAV] [HOPEA#]"])}
    ]
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
