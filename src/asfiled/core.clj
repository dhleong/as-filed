(ns ^{:author "Daniel Leong"
      :doc "Command-line interface"}
  asfiled.core
  (:require [asfiled
             [sink :as snk]
             [nyartcc-sink :refer [create-sink]]])
  (:gen-class))

(defn -main
  "Interactive command-line interface"
  [& args]
  (let [local (or (first args) "KLGA")
        sink (create-sink local)]
    (println "Ready at" local)
    (loop [input ""]
      (when-not (empty? input)
        (println "Process" input)
        ) 
      (recur (read-line)))))
