#!/usr/bin/env python3
"""Test-only inspector; never build or publish any unrelated accepted app."""
import inspect_battery_voice_candidate as inspector
inspector.APPS['cleaner'] = ('com.universalrp.cleansweep', '2.83', 88, 'live-guard-v2.82', 'CleanSweep-v2.82.apk',
    '3964b17235b107db1c7a07d92bd5d32fbe0bf29127eafcdd97d7f50f9aa677f5', 'CleanSweep-v2.83.apk')
if __name__ == '__main__':
    inspector.main('cleaner')
