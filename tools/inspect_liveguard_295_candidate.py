#!/usr/bin/env python3
"""Live Guard 2.95 only; verify against immutable public 2.94."""
import inspect_battery_voice_candidate as inspector
inspector.APPS['cleaner'] = ('com.universalrp.cleansweep', '2.95', 100, 'live-guard-v2.94', 'CleanSweep-v2.94.apk',
    'c26de0c09022ea5d05169c238f91308de367dc2e0ccb16c902699d17de34c195', 'CleanSweep-v2.95.apk')
if __name__ == '__main__':
    inspector.main('cleaner')
