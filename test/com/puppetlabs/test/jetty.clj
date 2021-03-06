(ns com.puppetlabs.test.jetty
  (:require [clj-http.client :as client]
            [ring.util.response :as rr])
  (:use [com.puppetlabs.jetty]
        [com.puppetlabs.puppetdb.testutils]
        [clojure.test]))

(deftest ciphers
  (testing "buggy JVMs should return a specific set of ciphers to use"
    (is (seq (acceptable-ciphers "1.7.0_20"))))

  (testing "last-known-good JVM version should return a nil set of ciphers"
    (is (nil? (acceptable-ciphers "1.7.0_05"))))

  (testing "unaffected JVM version should return a nil set of ciphers"
    (is (nil? (acceptable-ciphers "1.6.0_05")))))

(deftest compression
  (testing "should return"
    (let [body (apply str (repeat 1000 "f"))
          app  (fn [req]
                 (-> body
                     (rr/response)
                     (rr/status 200)
                     (rr/content-type "text/plain")
                     (rr/charset "UTF-8")))]
      (with-test-jetty app port
        (testing "a gzipped response when requests"
          ;; The client/get function asks for compression by default
          (let [resp (client/get (format "http://localhost:%d/" port))]
            (is (= (resp :body) body))
            (is (= (get-in resp [:headers "content-encoding"]) "gzip")
                (format "Expected gzipped response, got this response: %s" resp))))

        (testing "an uncompressed response by default"
          ;; The client/get function asks for compression by default
          (let [resp (client/get (format "http://localhost:%d/" port) {:decompress-body false})]
            (is (= (resp :body) body))
            ;; We should not receive a content-encoding header in the uncompressed case
            (is (nil? (get-in resp [:headers "content-encoding"]))
                (format "Expected uncompressed response, got this response: %s" resp))))))))
