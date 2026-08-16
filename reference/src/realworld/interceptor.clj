(ns realworld.interceptor
  (:require [exoscale.interceptor :as exoscale]))

(defn enqueue [context interceptors]
  (exoscale/enqueue context interceptors))

(defn execute
  ([context]
   (exoscale/execute context))
  ([context interceptors]
   (exoscale/execute context interceptors)))

(defn terminate [context]
  (exoscale/terminate context))
