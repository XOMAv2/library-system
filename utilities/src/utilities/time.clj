(ns utilities.time)

(defn now []
  (-> #_(.instant (java.time.Clock/systemUTC))
      (java.time.Instant/now)
      (java.util.Date/from)))

(defn date [year month day]
  (-> (java.time.LocalDate/of year month day)
      (.atStartOfDay (java.time.ZoneId/of "UTC"))
      (.toInstant)
      (java.util.Date/from)))

(defn datetime [year month day hour minute]
  (-> (java.time.LocalDateTime/of year month day hour minute)
      (.atZone (java.time.ZoneId/of "UTC"))
      (.toInstant)
      (java.util.Date/from)))

(defn year-start [d]
  (-> (.toInstant d)
      (.atZone (java.time.ZoneId/of "UTC"))
      (.with (java.time.temporal.TemporalAdjusters/firstDayOfYear))
      (.toInstant)
      (java.util.Date/from)))

(defn month-start [d]
  (-> (.toInstant d)
      (.atZone (java.time.ZoneId/of "UTC"))
      (.with (java.time.temporal.TemporalAdjusters/firstDayOfMonth))
      (.toInstant)
      (java.util.Date/from)))

(defn day-start [d]
  (-> (.toInstant d)
      (.atZone (java.time.ZoneId/of "UTC"))
      (.toLocalDate)
      (.atStartOfDay (java.time.ZoneId/of "UTC"))
      (.toInstant)
      (java.util.Date/from)))

(defn at-zone [datetime timezone]
  (-> (.toInstant datetime)
      (java.time.LocalDateTime/ofInstant (java.time.ZoneId/of timezone))
      (.atZone (java.time.ZoneId/of "UTC"))
      (.toInstant)
      (java.util.Date/from)))

(defn years
  "Obtains a time-independent Period representing a number of years."
  [n]
  (java.time.Period/ofYears n))

(defn months
  "Obtains a time-independent Period representing a number of months."
  [n]
  (java.time.Period/ofMonths n))

(defn weeks
  "Obtains a time-independent Period representing a number of weeks."
  [n]
  (java.time.Period/ofWeeks n))

(defn days
  "Obtains a time-independent Period representing a number of days."
  [n]
  (java.time.Period/ofDays n))

(defn hours
  "Obtains a time-based Duration representing a number of standard hours."
  [n]
  (java.time.Duration/ofHours n))

(defn minutes
  "Obtains a time-based Duration representing a number of standard minutes."
  [n]
  (java.time.Duration/ofMinutes n))

(defn add [date amount1 & amounts]
  (let [date (-> (.toInstant date)
                 (.atZone (java.time.ZoneId/of "UTC")))
        amounts (cons amount1 amounts)
        date (reduce #(.plus % %2) date amounts)]
    (-> (.toInstant date)
        (java.util.Date/from))))

(defn subtract [date amount1 & amounts]
  (let [date (-> (.toInstant date)
                 (.atZone (java.time.ZoneId/of "UTC")))
        amounts (cons amount1 amounts)
        date (reduce #(.minus % %2) date amounts)]
    (-> (.toInstant date)
        (java.util.Date/from))))
