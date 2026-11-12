#!/usr/bin/env python3
"""Live Guard 3.00 only; verify against immutable public 2.99."""
import inspect_battery_voice_candidate as inspector

# (package, version, code, baseline tag, baseline asset, baseline sha256, new asset)
# The baseline is the *published, immutable* 2.99 APK - the one the owner already has -
# so the candidate is proven to be a true same-key upgrade from it, not from a local build.
inspector.APPS['cleaner'] = ('com.universalrp.cleansweep', '3.00', 105, 'live-guard-v2.99', 'CleanSweep-v2.99.apk',
    'd11818d1539d10061bf76060874e84a4bdfc4628c1dee1a5389dd0f89fd88b05', 'CleanSweep-v3.00.apk')
if __name__ == '__main__':
    inspector.main('cleaner')
