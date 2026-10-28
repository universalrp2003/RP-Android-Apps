# Website app requests and replies

The bilingual request box at `#request-app` opens a GitHub Issue draft with the visitor's title and description. Visitors must sign in, review, and submit on GitHub. The static website has no message database and does not itself store or deliver the form. Native HTML GET submission works without JavaScript.

The owner can reply at https://github.com/universalrp2003/RP-Android-Apps/issues and visitors can follow the same issue. Requests/replies are public; the form warns against sharing OTPs, passwords, phone numbers, private SMS or banking information. No completion date is promised. Existing issues remain the canonical request history; the website does not claim to embed a live inbox.

The latest-download table was checked against public, non-draft, non-prerelease GitHub releases for all twelve apps. SMS Guard 1.1.9 and Voice Notes 1.2 are published; Radio 1.28 is now verified public after successful run 40612310475. Pending candidate versions are not advertised as available releases.

The release-evidence disclosure is collapsed at the bottom of main content, after the app cards and request form. Local checks: 116 Python tests and app-localization gates passed. The Radio publication run has completed successfully; the website changes can now be pushed without invalidating its source guard.
