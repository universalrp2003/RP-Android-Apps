#!/usr/bin/env python3
"""Live Guard 2.99 only; verify against immutable public 2.98."""
import inspect_battery_voice_candidate as inspector
inspector.APPS['cleaner'] = ('com.universalrp.cleansweep', '2.99', 104, 'live-guard-v2.98', 'CleanSweep-v2.98.apk',
    'cab58416b8bbe40f8800b8250b05045d04001055c648ff71ff5b1db112498d44', 'CleanSweep-v2.99.apk')
if __name__ == '__main__':
    inspector.main('cleaner')
