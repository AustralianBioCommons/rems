(ns rems.db.attachments
  (:require [clj-time.core :as time]
            [clojure.tools.logging :as log]
            [rems.common.attachment-util :as attachment-util]
            [rems.common.util :refer [fix-filename]]
            [rems.config :refer [env]]
            [rems.db.core :as db]
            [rems.multipart :refer [scan-for-malware]]
            [rems.util :refer [file-to-bytes]])
  (:import [rems PayloadTooLargeException UnsupportedMediaTypeException InvalidRequestException]))

(defn check-size
  [file]
  (when (= :too-large (:error file)) ; set already by the multipart upload wrapper
    (throw (PayloadTooLargeException. (str "File is too large")))))

(defn check-allowed-attachment
  [x]
  (let [filename (or (:attachment/filename x)
                     (:filename x)
                     x)]
    (when-not (attachment-util/allowed-extension? filename)
      (throw (UnsupportedMediaTypeException. (str "Unsupported extension: " filename))))))

(defn get-attachment [attachment-id]
  (when-let [{:keys [id userid type appid filename data]} (db/get-attachment {:id attachment-id})]
    (check-allowed-attachment filename)
    {:application/id appid
     :attachment/id id
     :attachment/user userid
     :attachment/filename filename
     :attachment/data data
     :attachment/type type}))

(defn check-for-malware-if-enabled [byte-array]
  (when-let [malware-scanner-path (:scanner-path (:malware-scanning env))]
    (let [scan (scan-for-malware malware-scanner-path byte-array)]
      (when (:logging (:malware-scanning env))
        (when (seq (:out scan)) (log/info (:out scan)))
        (when (seq (:err scan)) (log/error (:err scan))))
      (when (:detected scan)
        (throw (InvalidRequestException. "Malware detected"))))))

(defn get-attachments
  "Gets attachments without the data."
  []
  (for [{:keys [id userid type appid filename]} (db/get-attachments)]
    (do
      (check-allowed-attachment filename)
      {:application/id appid
       :attachment/id id
       :attachment/user userid
       :attachment/filename filename
       :attachment/type type})))

(defn get-attachment-metadata [attachment-id]
  (when-let [{:keys [id userid type appid filename]} (db/get-attachment-metadata {:id attachment-id})]
    {:application/id appid
     :attachment/id id
     :attachment/user userid
     :attachment/filename filename
     :attachment/type type}))

(defn get-attachments-for-application [application-id]
  (vec
   (for [{:keys [id filename type userid]} (db/get-attachments-for-application {:application-id application-id})]
     {:attachment/id id
      :attachment/user userid
      :attachment/filename filename
      :attachment/type type})))

(defn save-attachment!
  [{:keys [tempfile filename content-type]} user-id application-id]
  (check-allowed-attachment filename)
  (let [byte-array (file-to-bytes tempfile)
        _ (check-for-malware-if-enabled byte-array)
        filename (fix-filename filename (mapv :attachment/filename (get-attachments-for-application application-id)))
        id (:id (db/save-attachment! {:application application-id
                                      :user user-id
                                      :filename filename
                                      :type content-type
                                      :data byte-array}))]
    {:id id
     :success true}))

(defn update-attachment!
  "Updates the attachment, but does not modify the file data! Also does not \"fix the filename\"."
  [attachment]
  (check-allowed-attachment (:attachment/filename attachment))
  (db/update-attachment! {:id (:attachment/id attachment)
                          :application (:application/id attachment)
                          :user (:attachment/user attachment)
                          :filename (:attachment/filename attachment)
                          :type (:attachment/type attachment)})
  {:id (:attachment/id attachment)
   :success true})

(defn redact-attachment!
  "Updates the attachment by zeroing the file data."
  [attachment-id]
  (db/redact-attachment! {:id attachment-id})
  {:id attachment-id
   :success true})

(defn copy-attachment! [new-application-id attachment-id]
  (let [attachment (db/get-attachment {:id attachment-id})]
    (:id (db/save-attachment! {:application new-application-id
                               :user (:userid attachment)
                               :filename (:filename attachment)
                               :type (:type attachment)
                               :data (:data attachment)}))))

(defn delete-attachment! [attachment-id]
  (db/delete-attachment! {:id attachment-id}))

(defn create-license-attachment! [{:keys [userid filename content-type data]}]
  (:id (db/create-license-attachment! {:user userid
                                       :filename filename
                                       :type content-type
                                       :data data
                                       :start (time/now)})))

(defn remove-license-attachment! [attachment-id]
  (db/remove-license-attachment! {:id attachment-id}))

(defn get-license-attachment [attachment-id]
  (when-let [attachment (db/get-license-attachment {:attachmentId attachment-id})]
    {:attachment/filename (:filename attachment)
     :attachment/data (:data attachment)
     :attachment/type (:type attachment)}))
