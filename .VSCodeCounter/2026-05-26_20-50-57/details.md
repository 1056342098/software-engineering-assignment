# Details

Date : 2026-05-26 20:50:57

Directory /Users/shenyc/code/se/school_manager

Total : 104 files,  9622 codes, 35 comments, 1142 blanks, all 10799 lines

[Summary](results.md) / Details / [Diff Summary](diff.md) / [Diff Details](diff-details.md)

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [backend/.mvn/wrapper/maven-wrapper.properties](/backend/.mvn/wrapper/maven-wrapper.properties) | Java Properties | 3 | 0 | 1 | 4 |
| [backend/Dockerfile](/backend/Dockerfile) | Docker | 11 | 0 | 3 | 14 |
| [backend/mvnw.cmd](/backend/mvnw.cmd) | Batch | 139 | 26 | 25 | 190 |
| [backend/pom.xml](/backend/pom.xml) | XML | 108 | 0 | 4 | 112 |
| [backend/src/main/java/com/schoolmanager/backend/BackendApplication.java](/backend/src/main/java/com/schoolmanager/backend/BackendApplication.java) | Java | 12 | 0 | 5 | 17 |
| [backend/src/main/java/com/schoolmanager/backend/approval/ApprovalController.java](/backend/src/main/java/com/schoolmanager/backend/approval/ApprovalController.java) | Java | 249 | 0 | 27 | 276 |
| [backend/src/main/java/com/schoolmanager/backend/approval/ApprovalProcessProgressService.java](/backend/src/main/java/com/schoolmanager/backend/approval/ApprovalProcessProgressService.java) | Java | 185 | 0 | 24 | 209 |
| [backend/src/main/java/com/schoolmanager/backend/approval/ApprovalService.java](/backend/src/main/java/com/schoolmanager/backend/approval/ApprovalService.java) | Java | 411 | 0 | 47 | 458 |
| [backend/src/main/java/com/schoolmanager/backend/approval/entity/Approval.java](/backend/src/main/java/com/schoolmanager/backend/approval/entity/Approval.java) | Java | 122 | 0 | 42 | 164 |
| [backend/src/main/java/com/schoolmanager/backend/approval/entity/ApprovalAssignee.java](/backend/src/main/java/com/schoolmanager/backend/approval/entity/ApprovalAssignee.java) | Java | 64 | 0 | 21 | 85 |
| [backend/src/main/java/com/schoolmanager/backend/approval/entity/ApprovalAttachment.java](/backend/src/main/java/com/schoolmanager/backend/approval/entity/ApprovalAttachment.java) | Java | 77 | 0 | 26 | 103 |
| [backend/src/main/java/com/schoolmanager/backend/approval/entity/ApprovalLog.java](/backend/src/main/java/com/schoolmanager/backend/approval/entity/ApprovalLog.java) | Java | 59 | 0 | 19 | 78 |
| [backend/src/main/java/com/schoolmanager/backend/approval/entity/ApprovalProcessProgress.java](/backend/src/main/java/com/schoolmanager/backend/approval/entity/ApprovalProcessProgress.java) | Java | 92 | 0 | 33 | 125 |
| [backend/src/main/java/com/schoolmanager/backend/approval/entity/ApprovalStep.java](/backend/src/main/java/com/schoolmanager/backend/approval/entity/ApprovalStep.java) | Java | 86 | 0 | 29 | 115 |
| [backend/src/main/java/com/schoolmanager/backend/approval/repo/ApprovalAssigneeRepository.java](/backend/src/main/java/com/schoolmanager/backend/approval/repo/ApprovalAssigneeRepository.java) | Java | 17 | 0 | 7 | 24 |
| [backend/src/main/java/com/schoolmanager/backend/approval/repo/ApprovalAttachmentRepository.java](/backend/src/main/java/com/schoolmanager/backend/approval/repo/ApprovalAttachmentRepository.java) | Java | 10 | 0 | 5 | 15 |
| [backend/src/main/java/com/schoolmanager/backend/approval/repo/ApprovalLogRepository.java](/backend/src/main/java/com/schoolmanager/backend/approval/repo/ApprovalLogRepository.java) | Java | 7 | 0 | 4 | 11 |
| [backend/src/main/java/com/schoolmanager/backend/approval/repo/ApprovalProcessProgressRepository.java](/backend/src/main/java/com/schoolmanager/backend/approval/repo/ApprovalProcessProgressRepository.java) | Java | 9 | 0 | 5 | 14 |
| [backend/src/main/java/com/schoolmanager/backend/approval/repo/ApprovalRepository.java](/backend/src/main/java/com/schoolmanager/backend/approval/repo/ApprovalRepository.java) | Java | 20 | 0 | 10 | 30 |
| [backend/src/main/java/com/schoolmanager/backend/approval/repo/ApprovalStepRepository.java](/backend/src/main/java/com/schoolmanager/backend/approval/repo/ApprovalStepRepository.java) | Java | 7 | 0 | 4 | 11 |
| [backend/src/main/java/com/schoolmanager/backend/auth/AuthController.java](/backend/src/main/java/com/schoolmanager/backend/auth/AuthController.java) | Java | 56 | 0 | 8 | 64 |
| [backend/src/main/java/com/schoolmanager/backend/auth/dto/LoginRequest.java](/backend/src/main/java/com/schoolmanager/backend/auth/dto/LoginRequest.java) | Java | 20 | 0 | 8 | 28 |
| [backend/src/main/java/com/schoolmanager/backend/auth/dto/LoginResponse.java](/backend/src/main/java/com/schoolmanager/backend/auth/dto/LoginResponse.java) | Java | 40 | 0 | 12 | 52 |
| [backend/src/main/java/com/schoolmanager/backend/common/ApiException.java](/backend/src/main/java/com/schoolmanager/backend/common/ApiException.java) | Java | 11 | 0 | 4 | 15 |
| [backend/src/main/java/com/schoolmanager/backend/common/ApiResponse.java](/backend/src/main/java/com/schoolmanager/backend/common/ApiResponse.java) | Java | 9 | 0 | 3 | 12 |
| [backend/src/main/java/com/schoolmanager/backend/common/GlobalExceptionHandler.java](/backend/src/main/java/com/schoolmanager/backend/common/GlobalExceptionHandler.java) | Java | 43 | 0 | 9 | 52 |
| [backend/src/main/java/com/schoolmanager/backend/common/HealthController.java](/backend/src/main/java/com/schoolmanager/backend/common/HealthController.java) | Java | 10 | 0 | 3 | 13 |
| [backend/src/main/java/com/schoolmanager/backend/config/AppProperties.java](/backend/src/main/java/com/schoolmanager/backend/config/AppProperties.java) | Java | 58 | 0 | 19 | 77 |
| [backend/src/main/java/com/schoolmanager/backend/config/WebConfig.java](/backend/src/main/java/com/schoolmanager/backend/config/WebConfig.java) | Java | 15 | 0 | 3 | 18 |
| [backend/src/main/java/com/schoolmanager/backend/crypto/AesCryptoService.java](/backend/src/main/java/com/schoolmanager/backend/crypto/AesCryptoService.java) | Java | 61 | 0 | 11 | 72 |
| [backend/src/main/java/com/schoolmanager/backend/init/DataInitializer.java](/backend/src/main/java/com/schoolmanager/backend/init/DataInitializer.java) | Java | 121 | 0 | 15 | 136 |
| [backend/src/main/java/com/schoolmanager/backend/oplog/OperationLogService.java](/backend/src/main/java/com/schoolmanager/backend/oplog/OperationLogService.java) | Java | 29 | 0 | 6 | 35 |
| [backend/src/main/java/com/schoolmanager/backend/oplog/entity/OperationLog.java](/backend/src/main/java/com/schoolmanager/backend/oplog/entity/OperationLog.java) | Java | 63 | 0 | 22 | 85 |
| [backend/src/main/java/com/schoolmanager/backend/oplog/repo/OperationLogRepository.java](/backend/src/main/java/com/schoolmanager/backend/oplog/repo/OperationLogRepository.java) | Java | 5 | 0 | 3 | 8 |
| [backend/src/main/java/com/schoolmanager/backend/policy/PolicyController.java](/backend/src/main/java/com/schoolmanager/backend/policy/PolicyController.java) | Java | 81 | 0 | 13 | 94 |
| [backend/src/main/java/com/schoolmanager/backend/policy/PolicyService.java](/backend/src/main/java/com/schoolmanager/backend/policy/PolicyService.java) | Java | 265 | 0 | 18 | 283 |
| [backend/src/main/java/com/schoolmanager/backend/policy/entity/PolicyDoc.java](/backend/src/main/java/com/schoolmanager/backend/policy/entity/PolicyDoc.java) | Java | 81 | 0 | 27 | 108 |
| [backend/src/main/java/com/schoolmanager/backend/policy/entity/PolicyDocChunk.java](/backend/src/main/java/com/schoolmanager/backend/policy/entity/PolicyDocChunk.java) | Java | 45 | 0 | 13 | 58 |
| [backend/src/main/java/com/schoolmanager/backend/policy/repo/PolicyDocChunkRepository.java](/backend/src/main/java/com/schoolmanager/backend/policy/repo/PolicyDocChunkRepository.java) | Java | 30 | 0 | 10 | 40 |
| [backend/src/main/java/com/schoolmanager/backend/policy/repo/PolicyDocRepository.java](/backend/src/main/java/com/schoolmanager/backend/policy/repo/PolicyDocRepository.java) | Java | 12 | 0 | 5 | 17 |
| [backend/src/main/java/com/schoolmanager/backend/profile/ProfileController.java](/backend/src/main/java/com/schoolmanager/backend/profile/ProfileController.java) | Java | 55 | 0 | 9 | 64 |
| [backend/src/main/java/com/schoolmanager/backend/profile/ProfileService.java](/backend/src/main/java/com/schoolmanager/backend/profile/ProfileService.java) | Java | 159 | 0 | 20 | 179 |
| [backend/src/main/java/com/schoolmanager/backend/profile/entity/Student.java](/backend/src/main/java/com/schoolmanager/backend/profile/entity/Student.java) | Java | 60 | 0 | 19 | 79 |
| [backend/src/main/java/com/schoolmanager/backend/profile/entity/StudentProfile.java](/backend/src/main/java/com/schoolmanager/backend/profile/entity/StudentProfile.java) | Java | 47 | 0 | 15 | 62 |
| [backend/src/main/java/com/schoolmanager/backend/profile/entity/StudentSensitive.java](/backend/src/main/java/com/schoolmanager/backend/profile/entity/StudentSensitive.java) | Java | 82 | 0 | 28 | 110 |
| [backend/src/main/java/com/schoolmanager/backend/profile/entity/UserProfile.java](/backend/src/main/java/com/schoolmanager/backend/profile/entity/UserProfile.java) | Java | 49 | 0 | 15 | 64 |
| [backend/src/main/java/com/schoolmanager/backend/profile/repo/StudentProfileRepository.java](/backend/src/main/java/com/schoolmanager/backend/profile/repo/StudentProfileRepository.java) | Java | 7 | 0 | 4 | 11 |
| [backend/src/main/java/com/schoolmanager/backend/profile/repo/StudentRepository.java](/backend/src/main/java/com/schoolmanager/backend/profile/repo/StudentRepository.java) | Java | 7 | 0 | 4 | 11 |
| [backend/src/main/java/com/schoolmanager/backend/profile/repo/StudentSensitiveRepository.java](/backend/src/main/java/com/schoolmanager/backend/profile/repo/StudentSensitiveRepository.java) | Java | 7 | 0 | 4 | 11 |
| [backend/src/main/java/com/schoolmanager/backend/profile/repo/UserProfileRepository.java](/backend/src/main/java/com/schoolmanager/backend/profile/repo/UserProfileRepository.java) | Java | 7 | 0 | 4 | 11 |
| [backend/src/main/java/com/schoolmanager/backend/qa/QaController.java](/backend/src/main/java/com/schoolmanager/backend/qa/QaController.java) | Java | 57 | 0 | 13 | 70 |
| [backend/src/main/java/com/schoolmanager/backend/security/AuthUser.java](/backend/src/main/java/com/schoolmanager/backend/security/AuthUser.java) | Java | 49 | 0 | 13 | 62 |
| [backend/src/main/java/com/schoolmanager/backend/security/CurrentUser.java](/backend/src/main/java/com/schoolmanager/backend/security/CurrentUser.java) | Java | 26 | 0 | 6 | 32 |
| [backend/src/main/java/com/schoolmanager/backend/security/JwtAuthFilter.java](/backend/src/main/java/com/schoolmanager/backend/security/JwtAuthFilter.java) | Java | 49 | 0 | 8 | 57 |
| [backend/src/main/java/com/schoolmanager/backend/security/JwtService.java](/backend/src/main/java/com/schoolmanager/backend/security/JwtService.java) | Java | 39 | 0 | 8 | 47 |
| [backend/src/main/java/com/schoolmanager/backend/security/SecurityConfig.java](/backend/src/main/java/com/schoolmanager/backend/security/SecurityConfig.java) | Java | 41 | 0 | 5 | 46 |
| [backend/src/main/java/com/schoolmanager/backend/security/SysUserDetailsService.java](/backend/src/main/java/com/schoolmanager/backend/security/SysUserDetailsService.java) | Java | 19 | 0 | 5 | 24 |
| [backend/src/main/java/com/schoolmanager/backend/student/StudentController.java](/backend/src/main/java/com/schoolmanager/backend/student/StudentController.java) | Java | 39 | 0 | 7 | 46 |
| [backend/src/main/java/com/schoolmanager/backend/student/StudentService.java](/backend/src/main/java/com/schoolmanager/backend/student/StudentService.java) | Java | 25 | 0 | 7 | 32 |
| [backend/src/main/java/com/schoolmanager/backend/student/entity/ClassManager.java](/backend/src/main/java/com/schoolmanager/backend/student/entity/ClassManager.java) | Java | 52 | 0 | 14 | 66 |
| [backend/src/main/java/com/schoolmanager/backend/student/repo/ClassManagerRepository.java](/backend/src/main/java/com/schoolmanager/backend/student/repo/ClassManagerRepository.java) | Java | 10 | 0 | 4 | 14 |
| [backend/src/main/java/com/schoolmanager/backend/user/UserController.java](/backend/src/main/java/com/schoolmanager/backend/user/UserController.java) | Java | 28 | 0 | 7 | 35 |
| [backend/src/main/java/com/schoolmanager/backend/user/entity/SysRole.java](/backend/src/main/java/com/schoolmanager/backend/user/entity/SysRole.java) | Java | 44 | 0 | 14 | 58 |
| [backend/src/main/java/com/schoolmanager/backend/user/entity/SysUser.java](/backend/src/main/java/com/schoolmanager/backend/user/entity/SysUser.java) | Java | 94 | 0 | 31 | 125 |
| [backend/src/main/java/com/schoolmanager/backend/user/repo/SysRoleRepository.java](/backend/src/main/java/com/schoolmanager/backend/user/repo/SysRoleRepository.java) | Java | 7 | 0 | 4 | 11 |
| [backend/src/main/java/com/schoolmanager/backend/user/repo/SysUserRepository.java](/backend/src/main/java/com/schoolmanager/backend/user/repo/SysUserRepository.java) | Java | 12 | 0 | 5 | 17 |
| [backend/src/main/resources/application.properties](/backend/src/main/resources/application.properties) | Java Properties | 15 | 0 | 8 | 23 |
| [backend/src/main/resources/db/migration/V1\_\_init.sql](/backend/src/main/resources/db/migration/V1__init.sql) | MS SQL | 142 | 0 | 12 | 154 |
| [backend/src/main/resources/db/migration/V2\_\_user\_profile\_and\_class\_manager.sql](/backend/src/main/resources/db/migration/V2__user_profile_and_class_manager.sql) | MS SQL | 17 | 0 | 2 | 19 |
| [backend/src/main/resources/db/migration/V3\_\_approval\_multi\_assignee\_and\_attachments.sql](/backend/src/main/resources/db/migration/V3__approval_multi_assignee_and_attachments.sql) | MS SQL | 44 | 0 | 4 | 48 |
| [backend/src/main/resources/db/migration/V4\_\_party\_league\_stage\_progress.sql](/backend/src/main/resources/db/migration/V4__party_league_stage_progress.sql) | MS SQL | 21 | 0 | 2 | 23 |
| [docker-compose.yml](/docker-compose.yml) | YAML | 56 | 0 | 4 | 60 |
| [frontend/Dockerfile](/frontend/Dockerfile) | Docker | 11 | 0 | 3 | 14 |
| [frontend/eslint.config.js](/frontend/eslint.config.js) | JavaScript | 21 | 0 | 2 | 23 |
| [frontend/index.html](/frontend/index.html) | HTML | 13 | 0 | 1 | 14 |
| [frontend/nginx.conf](/frontend/nginx.conf) | Properties | 16 | 0 | 4 | 20 |
| [frontend/package-lock.json](/frontend/package-lock.json) | JSON | 2,796 | 0 | 1 | 2,797 |
| [frontend/package.json](/frontend/package.json) | JSON | 31 | 0 | 1 | 32 |
| [frontend/public/favicon.svg](/frontend/public/favicon.svg) | XML | 1 | 0 | 0 | 1 |
| [frontend/public/icons.svg](/frontend/public/icons.svg) | XML | 24 | 0 | 1 | 25 |
| [frontend/src/App.css](/frontend/src/App.css) | PostCSS | 158 | 0 | 27 | 185 |
| [frontend/src/App.tsx](/frontend/src/App.tsx) | TypeScript JSX | 33 | 0 | 2 | 35 |
| [frontend/src/api.ts](/frontend/src/api.ts) | TypeScript | 47 | 0 | 11 | 58 |
| [frontend/src/assets/react.svg](/frontend/src/assets/react.svg) | XML | 1 | 0 | 0 | 1 |
| [frontend/src/assets/vite.svg](/frontend/src/assets/vite.svg) | XML | 1 | 0 | 1 | 2 |
| [frontend/src/auth.tsx](/frontend/src/auth.tsx) | TypeScript JSX | 70 | 0 | 13 | 83 |
| [frontend/src/components/Layout.tsx](/frontend/src/components/Layout.tsx) | TypeScript JSX | 77 | 0 | 7 | 84 |
| [frontend/src/components/RequireAuth.tsx](/frontend/src/components/RequireAuth.tsx) | TypeScript JSX | 8 | 0 | 3 | 11 |
| [frontend/src/index.css](/frontend/src/index.css) | PostCSS | 198 | 0 | 35 | 233 |
| [frontend/src/main.tsx](/frontend/src/main.tsx) | TypeScript JSX | 15 | 0 | 2 | 17 |
| [frontend/src/pages/ApprovalApplyPage.tsx](/frontend/src/pages/ApprovalApplyPage.tsx) | TypeScript JSX | 183 | 0 | 16 | 199 |
| [frontend/src/pages/ApprovalsPage.tsx](/frontend/src/pages/ApprovalsPage.tsx) | TypeScript JSX | 291 | 0 | 26 | 317 |
| [frontend/src/pages/HomePage.tsx](/frontend/src/pages/HomePage.tsx) | TypeScript JSX | 183 | 3 | 18 | 204 |
| [frontend/src/pages/LoginPage.tsx](/frontend/src/pages/LoginPage.tsx) | TypeScript JSX | 53 | 0 | 4 | 57 |
| [frontend/src/pages/PendingPage.tsx](/frontend/src/pages/PendingPage.tsx) | TypeScript JSX | 250 | 0 | 20 | 270 |
| [frontend/src/pages/PolicyPage.tsx](/frontend/src/pages/PolicyPage.tsx) | TypeScript JSX | 279 | 1 | 18 | 298 |
| [frontend/src/pages/ProfilePage.tsx](/frontend/src/pages/ProfilePage.tsx) | TypeScript JSX | 389 | 0 | 29 | 418 |
| [frontend/src/pages/QaPage.tsx](/frontend/src/pages/QaPage.tsx) | TypeScript JSX | 68 | 0 | 7 | 75 |
| [frontend/src/pages/StudentDetailPage.tsx](/frontend/src/pages/StudentDetailPage.tsx) | TypeScript JSX | 158 | 0 | 13 | 171 |
| [frontend/src/pages/StudentsPage.tsx](/frontend/src/pages/StudentsPage.tsx) | TypeScript JSX | 77 | 0 | 10 | 87 |
| [frontend/tsconfig.app.json](/frontend/tsconfig.app.json) | JSON | 21 | 2 | 3 | 26 |
| [frontend/tsconfig.json](/frontend/tsconfig.json) | JSON with Comments | 7 | 0 | 1 | 8 |
| [frontend/tsconfig.node.json](/frontend/tsconfig.node.json) | JSON | 20 | 2 | 3 | 25 |
| [frontend/vite.config.ts](/frontend/vite.config.ts) | TypeScript | 13 | 1 | 2 | 16 |

[Summary](results.md) / Details / [Diff Summary](diff.md) / [Diff Details](diff-details.md)