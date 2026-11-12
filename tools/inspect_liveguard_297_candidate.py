#!/usr/bin/env python3
"""Live Guard 2.97 only; verify against immutable public 2.96."""
import inspect_battery_voice_candidate as inspector
inspector.APPS['cleaner'] = ('com.universalrp.cleansweep', '2.97', 102, 'live-guard-v2.96', 'CleanSweep-v2.96.apk',
    '92c7b0ffb4a2d714fe8dbcab190161558c06b7cb086754b1cbf61d30b2da384c', 'CleanSweep-v2.97.apk')
if __name__ == '__main__':
    inspector.main('cleaner')
