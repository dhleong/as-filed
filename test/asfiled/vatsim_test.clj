(ns asfiled.vatsim-test
  (:use org.httpkit.fake)
  (:require [clojure
             [string :refer [join]]
             [test :refer :all]]
            [asfiled.vatsim :refer :all]))

(def data-urls-raw
  (join "\n"
        ["url0=http://vatsim.aircharts.org/vatsim-data.txt" 
         "url0=http://vatsim-data.hardern.net/vatsim-data.txt"
         "url1=http://vatsim-data.hardern.net/vatsim-servers.txt"]))

(def mal-callsign "AAL1234")
(def mal (str mal-callsign ":1234567:Malcolm Reynolds - KMCO:PILOT::33.28706:-79.92776:35145:477:T/B738/F:470:KMIA:35000:KBOS:USA-E:100:1:1013:::3:I:0:7:0:0:0:0:KPVD:/t/:+HEDLY2 HEDLY J53 CRG J55 RDU J55 HPW PXT J191 RBV J222 JFK ROBUC1:0:0:0:0:::20150712010627:12:30.064:1018:"))

(def wash-callsign "AAL2345")
(def wash (str wash-callsign ":2345678:Hoban Washburne KAVL:::::::A321:415:KPIT:23000:KIAD:::::::0:I:125:125:0:32:2:9:KPIT:+VFPS+/V/PBN/A1B1C1D1O1S1 NAV/RNVD1E2A1 DOF/150712 REG/N321SB EET/KZDC0015 RMK/TCAS EQPT RMK/SIMBRIEF:MGW GIBBZ2:0:0:0:0:::::::"))

(def usa-east "USA-E:97.107.135.245:New Jersey, USA:USA East:1:")

(def data-raw
  (str "!GENERAL:\nVERSION = 8\nCONNECTED CLIENTS = 1\n;\n\n;\n!CLIENTS:\n" 
       mal "\n\n;\n!PREFILE:\n"
       wash "\n;"))

(def server-data-raw
  (str "!GENERAL:\nVERSION = 8\nCONNECTED CLIENTS = 0\n;\n\n;\n!CLIENTS:\n" 
       "\n\n;\n!PREFILE:\n\n\n;\n!SERVERS:\n"
       usa-east
       "\n;"))

(deftest into-seq-map-test
  (testing "Single element"
    (is (= {:url0 ["Foo Bar"]}
           (into-seq-map [["url0" "Foo Bar"]]))))
  (testing "Multi-element"
    (is (= {:url0 ["Foo" "Bar"]}
           (into-seq-map [["url0" "Foo"]
                          ["url0" "Bar"]])))))

(deftest data-urls-test
  (testing "Load data urls"
    (clear-data-urls-cache)
    (with-fake-http [#"status.vatsim.net$" data-urls-raw]
      (let [loaded (get-data-urls)]
        (is (= 2 (count loaded)))
        (is (= "http://vatsim.aircharts.org/vatsim-data.txt" (first loaded)))
        (is (= "http://vatsim-data.hardern.net/vatsim-data.txt" (second loaded))))
      (let [loaded (get-data-urls :type :server)]
        (is (= 1 (count loaded)))
        (is (= "http://vatsim-data.hardern.net/vatsim-servers.txt"
               (first loaded))))))
  (testing "Load from cache"
    (with-fake-http []
      (let [loaded (get-data-urls)]
        (is (= 2 (count loaded)))
        (is (= "http://vatsim.aircharts.org/vatsim-data.txt" (first loaded)))
        (is (= "http://vatsim-data.hardern.net/vatsim-data.txt" (second loaded)))))))

(deftest data-test
  (testing "Load data"
    (with-fake-http [#"status.vatsim.net" data-urls-raw
                     #"vatsim-data.txt$" data-raw]
      (let [loaded (load-data)]
        (is (= data-raw loaded)))))
  (testing "Parse data"
    (let [data (parse-data data-raw)]
      (is (= "8" (:version data)))
      (is (= "1" (:connected-clients data)))
      (is (= 1 (count (:clients data))))
      (is (= 1 (count (:prefile data))))
      (is (not (nil? (-> data :clients (get mal-callsign)))))
      (is (not (nil? (-> data :prefile (get wash-callsign))))))))

(deftest parse-client-data-test
  (testing "parse mal"
    (let [parsed (parse-client-data mal)]
      (is (= mal-callsign (:callsign parsed)))
      (is (= "KMIA" (:depart parsed)))
      (is (= "KBOS" (:arrive parsed)))
      (is (= 35000 (:cruise parsed)))
      (is (= "T/B738/F" (:craft parsed)))
      (is (= "+HEDLY2 HEDLY J53 CRG J55 RDU J55 HPW PXT J191 RBV J222 JFK ROBUC1" (:route parsed)))
      ))
 (testing "parse wash"
    (let [parsed (parse-client-data wash)]
      (is (= wash-callsign (:callsign parsed)))
      (is (= "KPIT" (:depart parsed)))
      (is (= "KIAD" (:arrive parsed)))
      (is (= 23000 (:cruise parsed)))
      (is (= "A321" (:craft parsed)))
      (is (= "MGW GIBBZ2" (:route parsed))))))

(deftest parse-server-data-test
  (testing "parse usa-east"
    (let [parsed (parse-server-data usa-east)]
      (is (= "USA East" (:name parsed)))
      (is (= "USA-E" (:id parsed)))
      (is (= "97.107.135.245" (:ip parsed)))
      (is (= "New Jersey, USA" (:location parsed))))))

(deftest getter-tests
  (testing "get-aircraft"
    (clear-data-cache)
    (with-fake-http [#"status.vatsim.net" data-urls-raw
                     #"vatsim-data.txt$" data-raw]
      (let [my-mal (get-aircraft mal-callsign)
            my-wash (get-aircraft wash-callsign)]
        ;; NB the actual parsing is tested above
        (is (not (nil? my-mal)))
        (is (not (nil? my-wash)))))
    (clear-data-cache))
  (testing "get-servers"
    (clear-data-cache)
    (with-fake-http [#"status.vatsim.net" data-urls-raw
                     #"vatsim-data.txt$" server-data-raw]
      (let [servers (get-servers)]
        ;; NB the actual parsing is tested above
        (is (= 1 (count servers)))
        (is (not (nil? (first servers))))))
    (clear-data-cache))
  (testing "get-servers :only?"
    (clear-data-cache)
    (with-fake-http [#"status.vatsim.net" data-urls-raw
                     #"vatsim-servers.txt$" server-data-raw]
      (let [servers (get-servers :only? true)]
        (is (= 1 (count servers)))
        (is (not (nil? (first servers))))))
    (clear-data-cache)))
