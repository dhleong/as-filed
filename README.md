# asfiled [![Build Status](http://img.shields.io/travis/dhleong/as-filed.svg?style=flat)](https://travis-ci.org/dhleong/as-filed)

asFiled is a cli app to make Clearance Delivery operations on Vatsim easier.

## Why?

Most of the information needed to clear aircraft at NYARTCC is available on
their website, but for new controllers it requires having a huge number of
tabs open at any given time. asFiled is designed to put as much relevant
information at the controller's fingertips as possible, while allowing the
controller to make up her own mind about how to issue the clearance. 

It is NOT intended to automatically generate correct clearances, as that 
would make it difficult for the controller to ever get their own solid grasp 
on their job.

It is also, of course, NOT intended for commercial use, but might prove a
useful training tool. Airport-specific data (like the SOPs) is designed to
be specifiable statically so new airports can be added without necessarily
needing to know how to program. The NYARTCC data source can be swapped out
for other data sources as needed.

## Installation and Usage

For now, just clone the repo somewhere and use [leiningen](http://leiningen.org/):

    lein run

This will default to the KLGA config. Other airports in the New York ARTCC
will probably work, but KLGA has the most features (including integrated
SOP). To start up for KJFK, for example, use:

    lein run KJFK

## Examples

asFiled takes input like a shell. By default, you can type in a VOR:

    Callsign/VOR: bdr
    Searching for bdr ...
    * VOR BDR 
      - Name: BRIDGEPORT
      - Freq: 108.800

Or an aircraft callsign on Vatsim:

Callsign/VOR: aal289
Searching for alx289 ...

    * AMERICAN AAL289 is:
      - T/B737/F
      - Large 2 Jet engines Boeing 737-700, BBJ, C-40 Airplane
      - RVSM capable with FMS (-> /L)
      - Mode C transponder
    * Traveling to Detroit Metropolitan Wayne County Airport (KDTW)
      - Via the WEST gate
      - Valid exits: [NEWEL ELIOT ZIMMZ! PARKE LANNA BIGGY SBJ]
      * On filed route:
        - LGA5.GAYEL J95 CFB CFB286 TRAAD ULW306 KOOPR YQO.SPICA2
        - Cruise at: 36000
    * Remarks:
      - /v/
    * PREFERRED ROUTES FROM KLGA
      - GAYEL J95 CFB TRAAD KOOPR YQO SPICA2 	 nil 	 nil
    * Current SID
      - JETS (South Gates) [CONEY]      [NTHNS#]
      - JETS (W/N/E Gates) [MASPETH]    [GLDMN#]
      - PROP (All Gates)   [FLUSHING**] [TNNIS#]

Preferred routes that are specific to the ARTCC will be denoted with
an asterisk instead of a dash.

If the aircraft is not found in the public Vatsim data, you may
manually input aircraft type and destination.

SID selection is initiated using the `.metar` command:

    Callsign/VOR: .metar
    KLGA 181851Z 20008KT 10SM SCT023 SCT028 BKN180 BKN250 28/21 A2991 RMK AO2 SLP127 T02830211
    * Min Flight Level: 190
    * Runways in use: (:lga-depart-13 :lga-land-22 :jfk-depart-22r :jfk-land-22)
      - LGA: Depart: 13  Land: VMC 22, IMC ILS22
      - JFK: Depart: 22R  Land: 22L
    * SID Selection:
      - JETS (South Gates) [CONEY]      [NTHNS#]
      - JETS (W/N/E Gates) [MASPETH]    [GLDMN#]
      - PROP (All Gates)   [FLUSHING**] [TNNIS#]

Runway tags may also be input manually if the SOP/METAR parsing didn't quite do the right thing:

    Callsign/VOR: .rwy lga-depart-13 lga-land-22 jfk-depart-22r jfk-land-22
    * For runway tags (:lga-depart-13 :lga-land-22 :jfk-depart-22r :jfk-land-22)
      - JETS (South Gates) [CONEY]      [NTHNS#]
      - JETS (W/N/E Gates) [MASPETH]    [GLDMN#]
      - PROP (All Gates)   [FLUSHING**] [TNNIS#]

## Acknowledgements

- Most of the data is read from [NYARTCC](http://nyartcc.org)
- Flight data from Vatsim is the property of [VATSIM.net](http://vatsim.net)
- Some data (especially bearing to destinations) from [SkyVector](https://skyvector.com)
- Preferred Routes not in the NYARTCC database from the [FAA Preferred Routes Database](http://www.fly.faa.gov/rmt/nfdc_preferred_routes_database.jsp)

## Disclaimer

The software is provided as-is. No warantee or guarantee of any kind is provided whatsoever. The author takes NO responsibility for any harm you may cause or incur as a result of using this software. Use at your own risk, etc.

Be sure to follow normal best practices for network security (IE: don't leave all your ports open to the universe, etc).

## License

Copyright © 2015 Daniel Leong

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
