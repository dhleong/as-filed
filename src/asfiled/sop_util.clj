(ns ^{:author "Daniel Leong"
      :doc "Utilities for SOPs"}
  asfiled.sop-util
  (:require [clojure.string :refer [join]]))

(defn- within-range
  [candidate range-array]
  (<= (first range-array) candidate (last range-array)))

(defn match-runway
  [weather runway-descriptor]
  (let [runway (:when runway-descriptor)]
    (and
      ;; speed 
      (within-range (:speed weather) (:speed runway))
      ;; direction
      (filter 
        #(within-range (:dir weather) %)
        (:dir runway))
      ;; rvr
      (or
        ;; eg: both nil
        (= (:rvr weather) (:rvr runway))
        ;; rvr range
        (within-range (:rvr weather) (:rvr runway))))))

(defn- has-tag
  [vect tag]
  (some #(= % tag) vect))

(defn match-tag
  [tag tag-descriptor]
  (if (map? tag-descriptor)
    ;; :any or :not
    (case (-> tag-descriptor keys first)
      :any (has-tag (:any tag-descriptor) tag)
      :not (not (has-tag (:not tag-descriptor) tag)))
    ;; exact match
    (= tag tag-descriptor)))

(defn match-sid
  [tags sid-descriptor]
  (let [tag-descriptor (:when sid-descriptor)]
    (when (= (count tags) (count tag-descriptor))
      (and (map #(match-tag %1 %2) tags tag-descriptor)))))

(defn select-runways
  "Given weather conditions and an SOP,
  figure out what runway configuration
  should be in use. 
  Weather should look like:
  {:speed 3 :dir 240 :rvr 3500}
  :rvr may be omitted if none specified.
  Returns:
  {:runways 'string description', :tags [:depart-1 :land-2]}"
  [sop weather]
  (when-let [runways (:runway-selection sop)]
    (when-let [matched (filter #(match-runway weather %) runways)]
      {:runways (join "\n" (map :use matched))
       :tags (apply concat (map :tags matched))})))

(defn select-sid
  "Given a set of tags (as returned from select-runways)
  and an SOP, figure out which SID should be used."
  [sop tags]
  (when-let [sids (:sid-selection sop)]
    (when-let [matched (filter #(match-sid tags %) sids)]
      (join "\n" (map :use matched)))))
