#!/usr/bin/env python3
"""Test-only inspector; never build or publish any unrelated accepted app."""
import inspect_battery_voice_candidate as inspector
inspector.APPS['cleaner'] = ('com.universalrp.cleansweep', '2.88', 93, 'live-guard-v2.87', 'CleanSweep-v2.87.apk',
    '31df0d1314f3e6a5d6d559dfac2dd96ff87a1c0cb60e03176000f4d8c907e715', 'CleanSweep-v2.88.apk')
if __name__ == '__main__':
    inspector.main('cleaner')
