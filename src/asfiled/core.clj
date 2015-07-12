(ns ^{:author "Daniel Leong"
      :doc "Command-line interface"}
  asfiled.core
  (:require [clojure.string :refer [upper-case]]
            [asfiled
             [api :refer [analyze]]
             [sink :as snk]
             [nyartcc-sink :refer [create-sink]]
             [vatsim :refer [get-aircraft]]])
  (:gen-class))

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

(defn handle-aircraft
  [sink client]
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
    ;; TODO it'd be nice if we could get the direction
    ;;  to the destination
    ;; prefered routes
    (when-let [routes (seq (-> data :preferred-routes))]
      (println "* PREFERRED ROUTES")
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
      (handle-aircraft sink
                       {:callsign (upper-case callsign)
                        :craft (upper-case craft)
                        :depart depart
                        :arrive arrive}))))

(defn -main
  "Interactive command-line interface"
  [& args]
  (let [local (or (first args) "KLGA")
        sink (create-sink local)]
    (println "Ready at" local)
    (loop [input ""]
      (when-not (empty? input)
        (println "Searching for" input "...")
        (if-let [live (get-aircraft (upper-case input))]
          (handle-aircraft sink live)
          (read-aircraft local sink input))) 
      (recur (prompt "Callsign")))))
