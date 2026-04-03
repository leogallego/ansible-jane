# API Contracts: Workflow Templates

**Feature**: 004-workflow-templates | **Date**: 2026-04-03

All endpoints use the existing base URL + `/api/v2/` prefix configured in `AapApiService`.
All requests include `Authorization: Bearer <TOKEN>` via the existing `AuthInterceptor`.

## Endpoints

### 1. List Workflow Job Templates

```
GET workflow_job_templates/
```

**Query Parameters** (same pattern as job_templates):

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | Int | 1 | Page number |
| page_size | Int | 25 | Results per page |
| search | String? | null | Name search filter |
| labels__name__icontains | String? | null | Label name filter |
| order_by | String | -modified | Sort order |

**Response**: `PaginatedResponse<WorkflowJobTemplate>`

```json
{
  "count": 42,
  "next": "https://aap.example.com/api/v2/workflow_job_templates/?page=2",
  "previous": null,
  "results": [
    {
      "id": 10,
      "name": "Deploy Full Stack",
      "description": "Deploys frontend and backend",
      "ask_variables_on_launch": true,
      "status": "successful",
      "last_job_run": "2026-04-01T12:00:00Z",
      "summary_fields": {
        "labels": {
          "count": 1,
          "results": [{"id": 1, "name": "production"}]
        },
        "user_capabilities": {
          "start": true
        }
      }
    }
  ]
}
```

### 2. Launch Workflow Job Template

```
POST workflow_job_templates/{id}/launch/
```

**Path Parameters**: `id` (Int) — workflow job template ID

**Request Body**: `LaunchRequest` (reused)

```json
{
  "extra_vars": "{\"env\": \"staging\"}"
}
```

**Response**: `WorkflowLaunchResponse`

```json
{
  "workflow_job": 123,
  "id": 123,
  "status": "pending"
}
```

**Error Responses**:
- 400: Invalid extra variables
- 403: User lacks launch permission
- 404: Template not found

### 3. Get Workflow Job Status

```
GET workflow_jobs/{id}/
```

**Path Parameters**: `id` (Int) — workflow job ID

**Response**: `WorkflowJob`

```json
{
  "id": 123,
  "name": "Deploy Full Stack",
  "status": "running",
  "failed": false,
  "started": "2026-04-03T10:00:00Z",
  "finished": null,
  "elapsed": 45.2,
  "summary_fields": {
    "workflow_job_template": {
      "id": 10,
      "name": "Deploy Full Stack"
    }
  }
}
```

### 4. Get Workflow Nodes (Sub-Jobs)

```
GET workflow_jobs/{id}/workflow_nodes/
```

**Path Parameters**: `id` (Int) — workflow job ID

**Response**: `PaginatedResponse<WorkflowNode>`

```json
{
  "count": 3,
  "next": null,
  "previous": null,
  "results": [
    {
      "id": 1,
      "do_not_run": false,
      "summary_fields": {
        "job": {
          "id": 456,
          "name": "Deploy Frontend",
          "status": "successful",
          "type": "job"
        }
      }
    },
    {
      "id": 2,
      "do_not_run": false,
      "summary_fields": {
        "job": {
          "id": 457,
          "name": "Deploy Backend",
          "status": "running",
          "type": "job"
        }
      }
    },
    {
      "id": 3,
      "do_not_run": true,
      "summary_fields": {
        "job": null
      }
    }
  ]
}
```

## Retrofit Interface Additions

```kotlin
// Add to AapApiService.kt:

@GET("workflow_job_templates/")
suspend fun getWorkflowJobTemplates(
    @Query("page") page: Int = 1,
    @Query("page_size") pageSize: Int = 25,
    @Query("search") search: String? = null,
    @Query("labels__name__icontains") labelsFilter: String? = null,
    @Query("order_by") orderBy: String = "-modified"
): PaginatedResponse<WorkflowJobTemplate>

@POST("workflow_job_templates/{id}/launch/")
suspend fun launchWorkflowJob(
    @Path("id") id: Int,
    @Body request: LaunchRequest = LaunchRequest()
): WorkflowLaunchResponse

@GET("workflow_jobs/{id}/")
suspend fun getWorkflowJob(@Path("id") id: Int): WorkflowJob

@GET("workflow_jobs/{id}/workflow_nodes/")
suspend fun getWorkflowNodes(
    @Path("id") id: Int,
    @Query("page_size") pageSize: Int = 200
): PaginatedResponse<WorkflowNode>
```
