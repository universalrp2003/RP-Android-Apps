#!/usr/bin/env python3
"""Live Guard 2.96 only; verify against immutable public 2.95."""
import inspect_battery_voice_candidate as inspector
inspector.APPS['cleaner'] = ('com.universalrp.cleansweep', '2.96', 101, 'live-guard-v2.95', 'CleanSweep-v2.95.apk',
    'b51d355d113511f45a6cf121fffc0765f188b578e69001e11e4f986f0a7eecb7', 'CleanSweep-v2.96.apk')
if __name__ == '__main__':
    inspector.main('cleaner')
