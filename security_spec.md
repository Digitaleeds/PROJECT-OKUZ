# Project Okuz — Firestore Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. **Authentication & Ownership Isolation**: Every storyboard page document at `/users/{userId}/pages/{pageId}` must belong exclusively to the authenticated user (`request.auth != null`, `request.auth.uid == userId`, and `data.userId == request.auth.uid`).
2. **Page Number Bounds**: `pageNumber` must be an integer between `1` and `25` inclusive (representing the 25-page Book One storyboard structure) and cannot be mutated after creation.
3. **Strict Key & Type Schema (`isValidStoryboardPage`)**: Only the 21 declared storyboard schema fields are permitted (`hasAll` and `hasOnly` on create; `affectedKeys().hasOnly(...)` on update). No shadow/ghost fields are allowed.
4. **Mandatory String Length Bounds**: Every string property (`mandate`, `cameraDistance`, `separationForce`, `storyline`, `illustration`, `kidPosture`, `dogAction`, `distanceApart`, `trigger`, `response`, `turningPoint`, `terrain`, `obstacle`, `sketchStrokes`) enforces strict `.size() <= MAX` boundaries to prevent Denial-of-Wallet payload bloat.
5. **Immutable Audit Metadata**: `userId`, `pageNumber`, and `createdAt` are immutable across updates (`incoming().createdAt == existing().createdAt`). Timestamps (`createdAt`, `updatedAt`) must be server timestamps (`is timestamp && <= request.time`).
6. **Query Enforcement**: `allow list` verifies `request.auth.uid == userId && existing().userId == request.auth.uid`, requiring client queries to filter with `.whereEqualTo("userId", userId)`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read/Write**: Attempt `get`, `list`, `create`, `update`, or `delete` with `request.auth == null`.
2. **Cross-User Read (`bob` reading `alice`)**: Authenticated as `bob_456`, attempt `get` or `list` on `/users/alice_123/pages/page_01`.
3. **Identity Spoofing on Create**: Authenticated as `alice_123`, write to `/users/alice_123/pages/page_01` with `userId: "bob_456"`.
4. **Shadow/Ghost Field Injection**: Send valid payload plus `"isAdmin": true` or `"isVerified": true` on create or update.
5. **Page Number Out of Range**: Attempt create with `pageNumber: 0` or `pageNumber: 26` or `pageNumber: "1"`.
6. **Immutable `createdAt` Mutation**: Attempt update changing `createdAt` to a newer timestamp.
7. **Immutable `userId` / `pageNumber` Transfer**: Attempt update changing `userId` or `pageNumber` on an existing document.
8. **Future Timestamp Forgery**: Attempt create or update with `updatedAt` set in the future (`> request.time`).
9. **Value Type Poisoning**: Attempt update setting `storyline: 99999` (int instead of string) or `completed: "yes"` (string instead of bool).
10. **Denial-of-Wallet Oversized String**: Attempt update with `mandate` exceeding 300 chars or `storyline` exceeding 5000 chars.
11. **Path ID Poisoning**: Attempt create at `/users/alice_123/pages/invalid id with spaces!`.
12. **Unfiltered Collection List Query**: Attempt `list` on `/users/alice_123/pages` without matching `userId` ownership constraint.
