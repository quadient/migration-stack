//! ---
//! displayName: Deploy Styles
//! category: Deployment
//! description: Deploys text and paragraph style to external style definition
//! ---
package com.quadient.migration.example.common

import static com.quadient.migration.example.common.util.InitMigration.initMigration

initMigration(this.binding).deployClient.deployStyles()