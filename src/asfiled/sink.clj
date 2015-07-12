(ns ^{:author "Daniel Leong"
      :doc "Data sink protocol"}
  asfiled.sink)

(defprotocol Sink
  "Protocol for fetching data needed in asFiled.
  All methods block; async handling can be done elsewhere"
  (get-aircraft
    [this aircraft-type]
    "Load aircraft info for the given type, eg: A321.
    Resolves to a map:
    {:model 'A-321'
     :manufacturer 'Airbus'
     :type 'Airplane'
     :engines '2 Jet engines'
     :weight 'Large'}")
  (get-airline 
    [this airline-abbr] 
    "Load airline info for the given airline abbrv, eg: BAW.
    Resolves to a map:
    {:name 'British Airways'
     :telephony 'Speedbird'}")
  (get-airport
    [this airport-icao]
    "Load airport info for the given airport ICAO, eg: KIAD.
    Resolves to a map:
    {:icao 'KIAD' :name 'Washington Dulles International Aiport'}")
  (get-preferred-routes
    [this depart arrive]
    "Given ICAO codes for departure and arrival airports,
    return a list of preferred routes (if any).
    Resolves to a list of maps:
    [{:route 'MERIT ROBUC#'
       :area ''
       :altitude '110-FL210'
       :aircraft 'TURBOJET RNAV ONLY'
       :preferred true}]")
  (get-valid-exits
    [this arrive]
    "Given ICAO codes for arrival airports,
    (and assuming a knowledge of the departure airpot)
    return a map describing the expected exits:
    {:gate :east
     :bearing 90
     :exits ['MERIT', 'GREKI']}
    This method is optional; if no SOP for the facility
    is available, you may return nil. If you don't have
    the SOP but can calculate the bearing, it is acceptable
    to return only eg {:bearing 90}"))
