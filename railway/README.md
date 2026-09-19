# Smart SMS Reply licensing service

This folder is deployed as a separate Railway service with a Railway PostgreSQL database.

Required environment variables:

- `DATABASE_URL`: supplied by the Railway PostgreSQL service
- `ADMIN_TOKEN`: a long random secret used only by the app owner
- `LICENSE_PEPPER`: a separate long random secret used to hash licence keys
- `PGSSLMODE=disable`: use only when Railway's private database URL requires it

Never commit the values above or an Android signing keystore to GitHub.

The public Android app calls `/v1/licenses/activate` and `/v1/licenses/validate`.
Only the owner/admin sales tool calls `/v1/admin/licenses`.
