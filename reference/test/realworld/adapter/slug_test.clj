(ns realworld.adapter.slug-test
  (:require [lazytest.core :refer [defdescribe expect it]]
            [realworld.adapter.slug :as slug]))

(def id
  (parse-uuid "a1b2c3d4-0000-0000-0000-000000000000"))

(defdescribe slug-adapter
  (it "creates a URL-safe slug with a short random suffix"
    (expect (= "hello-world-a1b2c3"
               (slug/from-title "Hello, World!" id))))

  (it "provides a base when the title has no URL-safe characters"
    (expect (= "article-a1b2c3"
               (slug/from-title "?!" id)))))
