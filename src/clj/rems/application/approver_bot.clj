(ns rems.application.approver-bot
  (:require [clj-time.core :as time]
            [rems.common.application-util :as application-util]
            [rems.db.applications]))

(def bot-userid "approver-bot")

(defn- should-approve? [application]
  (empty? (:application/blacklist application)))

(defn- generate-commands [event application]
  (when (and (application-util/is-handler? application bot-userid)
             (should-approve? application))
    [{:type :application.command/approve
      :actor bot-userid
      :time (time/now)
      :application-id (:application/id event)
      :comment ""}]))

(defn- maybe-generate-commands [event]
  (when (= :application.event/submitted (:event/type event)) ; approver bot only reacts to fresh applications
    (let [application (rems.db.applications/get-application (:application/id event))]
      (generate-commands event application))))

(defn run-approver-bot [new-events]
  (doall (mapcat #(maybe-generate-commands %)
                 new-events)))
