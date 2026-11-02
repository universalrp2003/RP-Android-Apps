#!/usr/bin/env python3
"""Test-only inspector; never build or publish any unrelated accepted app."""
import inspect_battery_voice_candidate as inspector
inspector.APPS['cleaner'] = ('com.universalrp.cleansweep', '2.89', 94, 'live-guard-v2.88', 'CleanSweep-v2.88.apk',
    '87be21168e9f7d619ee8709163f632ebfd79b1a731d2b45e8b4d6618ca43a70c', 'CleanSweep-v2.89.apk')
if __name__ == '__main__':
    inspector.main('cleaner')
