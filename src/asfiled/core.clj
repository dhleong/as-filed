(ns ^{:author "Daniel Leong"
      :doc "Command-line interface"}
  asfiled.core
  (:require [clojure.string :refer [upper-case]]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [asfiled
             [api :refer [analyze]]
             [sink :as snk]
             [skyvector :refer [get-vor]]
             [nyartcc-sink :refer [create-sink]]
             [vatsim :refer [get-aircraft]]])
  (:gen-class))

;;
;; Constants
;;

(def prompt-text "Callsign/VOR")
(def nrepl-port 7888)

;;
;; Util methods
;;

(defn- prompt
  [prompt-msg]
  (print (str prompt-msg ": "))
  (flush)
  (read-line))

(defn- pad
  [input length]
  (if (> length 0)
    (format (str "%-" length "s") input)
    input))

(defn- longest-string
  [items selector]
  (reduce max (map #(count (get % selector)) items)))

;;
;; Handler functions, possibly testable
;;

(defn handle-aircraft
  [local sink client]
  (let [data (analyze sink client)
        craft (-> data :craft)
        equip (-> data :equip)]
    (println "\n*" 
             (-> data :airline :telephony) 
             (:callsign client) 
             "is:")
    (println "  -" (:craft client))
    (println "  -" 
             (:weight craft)
             (:engines craft) 
             (:manufacturer craft)
             (:model craft)
             (:type craft))
    (when equip
      (if (-> equip :rvsm)
        (println "  - RVSM capable with" (-> equip :nav))
        (println "  - NOT RVSM with" (-> equip :nav)))
      (case (-> equip :transponder)
        :none (println "  - NO transponder")
        :no-mode-c (println "  - Non-Mode C transponder")
        :mode-c (println "  - Mode C transponder")
        (println "  - (transponder info unknown)")))
    (when-not equip
      (println "  - NO equipment type specified"))
    ;; destination
    (println "* Traveling to" 
             (-> data :arrive :name) 
             (str "(" (-> data :arrive :icao) ")"))
    (when-let [exits (-> data :exits)]
      (if-let [gate (:gate exits)]
        (println "  - Via the" (upper-case (name gate)) "gate")
        (println "  - Bearing" (:bearing exits)))
      (if-let [exit-points (:exits exits)]
        (println "  - Valid exits:" exit-points)))
    ;; prefered routes
    (when-let [routes (seq (-> data :preferred-routes))]
      (println "* PREFERRED ROUTES FROM" (upper-case local))
      (let [longest-route (longest-string routes :route)
            longest-alt (longest-string routes :altitude)]
        (doseq [route routes]
          (if (:preferred route)
            (print "  * ")
            (print "  - "))
          (println (pad (:route route) longest-route)
                   "\t"
                   (pad (:altitude route) longest-alt)
                   "\t"
                   (:aircraft route)))))))

(defn read-aircraft
  [depart sink callsign]
  (println "Callsign" callsign "not found.")
  (println "Please provide...")
  (if-let [craft (prompt "Aircraft")]
    (if-let [arrive (prompt "Destination")]
      (handle-aircraft depart
                       sink
                       {:callsign (upper-case callsign)
                        :craft (upper-case craft)
                        :depart depart
                        :arrive arrive}))))

;;
;; Cli handlers
;; They should accept [sink input]
;;

(defn- cli-vor
  [sink input]
  (let [local (snk/get-facility sink)]
    (if-let [vor (get-vor local input)]
      (do (println "* VOR" (:id vor))
          (println "  - Name:" (:name vor))
          (println "  - Freq:" (:freq vor)))
      (println "! No result for" input))))

(defn- cli-aircraft 
  [sink input]
  (let [local (snk/get-facility sink)]
    (if-let [live (get-aircraft (upper-case input))]
      (handle-aircraft local sink live)
      (read-aircraft local sink input))))

(defn- pick-cli-handler [input]
  (cond 
    (= 3 (count input)) cli-vor
    :else cli-aircraft))

(defn -main
  "Interactive command-line interface"
  [& args]
  ;; repl
  (println "Init Repl...")
  (defonce nrepl-server (start-server :port nrepl-port))
  (println "Repl available on" nrepl-port)
  ;; use defs so we can re-def via repl if desired
  (def local (or (first args) "KLGA"))
  (def sink (create-sink local))
  ;; now do the loop!
  (println "Ready at" local)
  (loop [input ""]
    (when-not (empty? input)
      (let [handler (pick-cli-handler input)]
        (println "Searching for" input "...")
        (handler sink input))) 
    (recur (prompt prompt-text))))
