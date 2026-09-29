
To preserve user edits while continually updating a generated Postman collection from your Spring controllers, you need to implement a **Three-Way Merge** architecture.

If your generator simply reads the Spring code and overwrites the output file, it performs a destructive 2-way sync. A 3-way merge introduces a "memory" of what was generated last time, allowing you to mathematically determine which changes came from the code (which you want to apply) and which came from the user (which you want to keep).

## The Three-Way Merge Architecture

You need to track three distinct states of your collection to safely merge updates:
1. **The Base (`last_generated_state.json`)**: The pure, unedited output of your _previous_ generator run.  
2. **The Target (`current_user_collection.json`)**: The collection currently sitting in Postman, which contains the user's manual edits (custom payloads, auth headers, variables).  
3. **The Update (`newly_generated_state.json`)**: The pure output of your _current_ generator run, reflecting your latest Spring controller code.  

By comparing these three states, your tool can safely resolve updates:
- If a field changed between `Base` and `Target`, the **User** changed it. Preserve it.    
- If a field changed between `Base` and `Update`, the **Generator** changed it. Apply it. 
- If an endpoint exists in `Update` but not `Base`, it's a new Spring endpoint. Add it.
- If an endpoint exists in `Base` but not `Update`, it was deleted from Spring. Remove it (or deprecate it).
   
## Implementation Strategy

To make this work, you need a persistent caching mechanism and a set of field-level resolution rules.
### 1. The Storage Mechanism

Store a **shadow file** (e.g., `.generator-cache.json`) alongside your source code or in a local SQLite database.
- When a user triggers the generator, it first fetches the `current_user_collection.json` via the Postman API (or prompts the user to export it).
- The generator builds the `newly_generated_state.json` in memory.
-  The merge script runs, comparing the three files.
- The merged result is pushed to Postman (or output to a file), and the `.generator-cache.json` is overwritten with the _pure_ `newly_generated_state.json` to act as the base for the next run.

### 2. The Unique Identifier (Primary Key)

Postman collections use hierarchical folders and request names. Your generator must assign a deterministic, invisible ID to every endpoint to track it reliably even if the user renames the request in Postman.
- **Best approach:** Inject a custom header, a Postman description block, or use the `id` field in the Postman schema based on the HTTP Method + Path (e.g., `POST:/api/v1/users`). This acts as your primary key for matching requests during the merge.

### 3. Field-Level Ownership Rules

When resolving the merge, you cannot apply a blanket "keep user changes" rule, because structural changes from your Spring controllers must take precedence. Implement granular rules based on the JSON structure of a Postman collection.
- **Generator Owns (Always Overwrite):**      
    - URL paths and HTTP methods (e.g., changing `/api/v1/user` to `/api/v2/user`).
    - Available query parameters derived from `@RequestParam`. 
    - Content-Type headers derived from `produces`/`consumes`.
- **User Owns (Always Preserve):**
    - The `value` of parameters, headers, and authorization tokens.
    - The raw JSON body payload (the user likely pasted in valid test data).
    - Pre-request scripts and test scripts.
    - Environment variable references (e.g., `{{baseUrl}}`).

### 4. Merging the Request Body

The most common user edit is modifying the request body. If the user changes `{"name": "string"}` to `{"name": "John"}` in Postman, you must preserve "John". However, if your Spring controller adds a new field (`age`), you need to inject it.

To achieve this, parse the JSON body of both the `Target` (User) and `Update` (Generator).
- Iterate through the `Update` payload keys.
- If the key exists in the `Target` payload, keep the `Target` value.
- If the key does not exist in the `Target` payload, insert it with your default generated value.

## Alternative: The Patch/Overlay Approach

If building a 3-way merge is too complex, a simpler architectural alternative is the **Overlay Model**:
1. Your Spring generator outputs a read-only `collection.json` that users are instructed _never_ to edit directly.
2. Users create a separate `user-overrides.json` file where they define their custom payloads, headers, and scripts mapped by the endpoint URL (e.g., `POST:/users`).
	1. Your tool reads both files during generation and deeply merges `user-overrides.json` on top of `collection.json` to produce the final output.

This is easier to build than a 3-way merge but slightly degrades the user experience, as they can no longer natively edit test data directly inside the Postman UI and expect it to persist without copying it to the overrides file.