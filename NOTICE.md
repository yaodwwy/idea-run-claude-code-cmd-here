# Third-Party Notices

## AI Git Commit (com.hmydk.aigit)

The `com.hmydk.aigit` package and the `icons/git-commit-logo.svg` asset in this
plugin are derived from the **AIGitCommit** project:

- Source: https://github.com/HMYDK/AIGitCommit
- Author: HMYDK
- License: GNU Affero General Public License v3.0 (AGPL-3.0)

> AIGitCommit switched from Apache 2.0 to AGPL-3.0 starting at v1.5.4.
> The code included here is taken from the AGPL-3.0 licensed version.

### What this means

AGPL-3.0 is a strong copyleft license. The files under
`src/main/java/com/hmydk/aigit/` and `src/main/resources/icons/git-commit-logo.svg`
remain covered by AGPL-3.0. You may freely use, modify and distribute them
(including in this combined plugin), **provided that**:

- you keep the source code of these files and any modifications available under
  AGPL-3.0, and
- if you offer the combined work as a network service (SaaS), you must also make
  the corresponding source available to users of that service.

See `LICENSE-AGPL-3.0` (the AGPL-3.0 full text, or the upstream LICENSE) for
the complete terms. The rest of this plugin's own code remains under the MIT
License (see `LICENSE`).

### Attribution

The "Generate AI Commit Message" action, the prompt/model configuration UI and
all LLM service implementations (`AIService`, `CommitMessageService`, the
`service/impl/*` providers, `util/*` helpers and `context/*` models) originate
from AIGitCommit by HMYDK. Please direct upstream issues/improvements to the
original repository.
