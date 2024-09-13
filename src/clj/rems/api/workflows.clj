(ns rems.api.workflows
  (:require [compojure.api.sweet :refer :all]
            [rems.api.schema :as schema]
            [rems.api.util :refer [not-found-json-response extended-logging]]
            [rems.common.application-util :as application-util] ; required for route :roles
            [rems.common.roles :refer [+admin-read-roles+ +admin-write-roles+]]
            [rems.schema-base :as schema-base]
            [rems.service.users]
            [rems.service.workflow :as workflow]
            [ring.util.http-response :refer [ok]]
            [schema.core :as s]))

(s/defschema CreateWorkflowCommand
  {:organization schema-base/OrganizationId
   :title s/Str
   (s/optional-key :forms) [{:form/id s/Int}]
   :type (apply s/enum application-util/workflow-types) ; TODO: exclude master workflow?
   (s/optional-key :handlers) [schema-base/UserId]
   (s/optional-key :licenses) [schema-base/LicenseId]
   (s/optional-key :disable-commands) [schema-base/DisableCommandRule]
   (s/optional-key :voting) (s/maybe schema-base/WorkflowVoting)
   (s/optional-key :anonymize-handling) (s/maybe s/Bool)
   (s/optional-key :processing-states) [schema-base/ProcessingState]})

(s/defschema EditWorkflowCommand
  {:id s/Int
   (s/optional-key :organization) schema-base/OrganizationId
   ;; type can't change
   (s/optional-key :title) s/Str
   (s/optional-key :handlers) [schema-base/UserId]
   (s/optional-key :disable-commands) [schema-base/DisableCommandRule]
   (s/optional-key :voting) (s/maybe schema-base/WorkflowVoting)
   (s/optional-key :anonymize-handling) (s/maybe s/Bool)
   (s/optional-key :processing-states) [schema-base/ProcessingState]})

(s/defschema CreateWorkflowResponse
  {:success s/Bool
   (s/optional-key :id) s/Int
   (s/optional-key :errors) [s/Any]})

; TODO: deduplicate or decouple with /api/applications/reviewers API?
(s/defschema AvailableActor schema-base/UserWithAttributes)
(s/defschema AvailableActors [AvailableActor])

(defn- get-available-actors [] (rems.service.users/get-users))

(def workflows-api
  (context "/workflows" []
    :tags ["workflows"]

    (GET "/" []
      :summary "Get workflows"
      :roles +admin-read-roles+
      :query-params [{disabled :- (describe s/Bool "whether to include disabled workflows") false}
                     {archived :- (describe s/Bool "whether to include archived workflows") false}]
      :return [schema/Workflow]
      (ok (workflow/get-workflows (merge (when-not disabled {:enabled true})
                                         (when-not archived {:archived false})))))

    (POST "/create" request
      :summary "Create workflow"
      :roles +admin-write-roles+
      :body [command CreateWorkflowCommand]
      :return CreateWorkflowResponse
      (extended-logging request)
      (ok (workflow/create-workflow! command)))

    (PUT "/edit" request
      :summary "Edit workflow title and handlers"
      :roles +admin-write-roles+
      :body [command EditWorkflowCommand]
      :return schema/SuccessResponse
      (extended-logging request)
      (ok (workflow/edit-workflow! command)))

    (PUT "/archived" request
      :summary "Archive or unarchive workflow"
      :roles +admin-write-roles+
      :body [command schema/ArchivedCommand]
      :return schema/SuccessResponse
      (extended-logging request)
      (ok (workflow/set-workflow-archived! command)))

    (PUT "/enabled" request
      :summary "Enable or disable workflow"
      :roles +admin-write-roles+
      :body [command schema/EnabledCommand]
      :return schema/SuccessResponse
      (extended-logging request)
      (ok (workflow/set-workflow-enabled! command)))

    (GET "/actors" []
      :summary "List of available actors"
      :roles +admin-write-roles+
      :return AvailableActors
      (ok (get-available-actors)))

    (GET "/:workflow-id" []
      :summary "Get workflow by id"
      :roles +admin-read-roles+
      :path-params [workflow-id :- (describe s/Int "workflow-id")]
      :return schema/Workflow
      (if-some [wf (workflow/get-workflow workflow-id)]
        (ok wf)
        (not-found-json-response)))))
