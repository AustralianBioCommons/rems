(ns rems.api.user-settings
  (:require [compojure.api.sweet :refer :all]
            [rems.api.schema :as schema]
            [rems.api.util :refer [extended-logging]]
            [rems.service.user-settings]
            [rems.util :refer [getx-user-id get-user-id]]
            [ring.util.http-response :refer :all]
            [schema.core :as s]))

(def GetUserSettings rems.service.user-settings/UserSettings)

(s/defschema UpdateUserSettings
  {(s/optional-key :language) s/Keyword
   (s/optional-key :notification-email) (s/maybe s/Str)})

(def user-settings-api
  (context "/user-settings" []
    :tags ["user-settings"]

    (GET "/" []
      :summary "Get user settings"
      :roles #{:logged-in}
      :return GetUserSettings
      (ok (rems.service.user-settings/get-user-settings (get-user-id))))

    (PUT "/edit" request
      :summary "Update user settings"
      :roles #{:logged-in}
      :body [settings UpdateUserSettings]
      :return schema/SuccessResponse
      (extended-logging request)
      (ok (rems.service.user-settings/update-user-settings! (getx-user-id) settings)))

    (PUT "/" request
      :summary "Update user settings, DEPRECATED, will disappear, use /edit instead"
      :roles #{:logged-in}
      :body [settings UpdateUserSettings]
      :return schema/SuccessResponse
      (extended-logging request)
      (ok (rems.service.user-settings/update-user-settings! (getx-user-id) settings)))))
