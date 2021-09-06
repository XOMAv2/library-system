(ns utilities.time
  (:refer-clojure :exclude [< <= > >=])
  (:import [java.util Date]
           [java.time LocalDateTime ZoneId ZonedDateTime Period Duration]))

#_(set! *warn-on-reflection* false)
#_(set! *warn-on-reflection* true)

(defn now []
  (-> #_(.instant (java.time.Clock/systemUTC))
      (java.time.Instant/now)
      (Date/from)))

(defn date [^Integer year ^Integer month ^Integer day]
  (-> (java.time.LocalDate/of year month day)
      (.atStartOfDay (ZoneId/of "UTC"))
      (.toInstant)
      (Date/from)))

(defn datetime [^Integer year ^Integer month ^Integer day
                ^Integer hour ^Integer minute]
  (-> (LocalDateTime/of year month day hour minute)
      (.atZone (ZoneId/of "UTC"))
      (.toInstant)
      (Date/from)))

(defn year-start [^Date d]
  (-> (.toInstant d)
      (.atZone (ZoneId/of "UTC"))
      (.with (java.time.temporal.TemporalAdjusters/firstDayOfYear))
      (.toInstant)
      (Date/from)))

(defn month-start [^Date d]
  (-> (.toInstant d)
      (.atZone (ZoneId/of "UTC"))
      (.with (java.time.temporal.TemporalAdjusters/firstDayOfMonth))
      (.toInstant)
      (Date/from)))

(defn day-start [^Date d]
  (-> (.toInstant d)
      (.atZone (ZoneId/of "UTC"))
      (.toLocalDate)
      (.atStartOfDay (ZoneId/of "UTC"))
      (.toInstant)
      (Date/from)))

(defn at-zone [^Date datetime timezone]
  (-> (.toInstant datetime)
      (LocalDateTime/ofInstant (ZoneId/of timezone))
      (.atZone (ZoneId/of "UTC"))
      (.toInstant)
      (Date/from)))

(defn years
  "Obtains a time-independent Period representing a number of years."
  [n]
  (Period/ofYears n))

(defn months
  "Obtains a time-independent Period representing a number of months."
  [n]
  (Period/ofMonths n))

(defn weeks
  "Obtains a time-independent Period representing a number of weeks."
  [n]
  (Period/ofWeeks n))

(defn days
  "Obtains a time-independent Period representing a number of days."
  [n]
  (Period/ofDays n))

(defn hours
  "Obtains a time-based Duration representing a number of standard hours."
  [n]
  (Duration/ofHours n))

(defn minutes
  "Obtains a time-based Duration representing a number of standard minutes."
  [n]
  (Duration/ofMinutes n))

(defn add [^Date date amount1 & amounts]
  (let [date (-> (.toInstant date)
                 (.atZone (ZoneId/of "UTC")))
        amounts (cons amount1 amounts)
        ^ZonedDateTime date (reduce #(.plus % %2) date amounts)]
    (-> (.toInstant date)
        (Date/from))))

(defn subtract [^Date date amount1 & amounts]
  (let [date (-> (.toInstant date)
                 (.atZone (ZoneId/of "UTC")))
        amounts (cons amount1 amounts)
        ^ZonedDateTime date (reduce #(.minus % %2) date amounts)]
    (-> (.toInstant date)
        (Date/from))))

(defn <
  ([^Date d1] true)
  ([^Date d1 ^Date d2] (.before  d1 d2))
  ([^Date d1 ^Date d2 & more]
   (if (< d1 d2)
     (if (next more)
       (recur d2 (first more) (next more))
       (< d2 (first more)))
     false)))

(defn <=
  ([^Date d1] true)
  ([^Date d1 ^Date d2] (or (.before d1 d2) (= d1 d2)))
  ([^Date d1 ^Date d2 & more]
   (if (<= d1 d2)
     (if (next more)
       (recur d2 (first more) (next more))
       (<= d2 (first more)))
     false)))

(defn >
  ([^Date d1] true)
  ([^Date d1 ^Date d2] (.after d1 d2))
  ([^Date d1 ^Date d2 & more]
   (if (> d1 d2)
     (if (next more)
       (recur d2 (first more) (next more))
       (> d2 (first more)))
     false)))

(defn >=
  ([^Date d1] true)
  ([^Date d1 ^Date d2] (or (.after d1 d2) (= d1 d2)))
  ([^Date d1 ^Date d2 & more]
   (if (>= d1 d2)
     (if (next more)
       (recur d2 (first more) (next more))
       (>= d2 (first more)))
     false)))
