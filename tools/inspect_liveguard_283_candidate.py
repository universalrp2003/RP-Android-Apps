#!/usr/bin/env python3
"""Test-only inspector; never build or publish any unrelated accepted app."""
import inspect_battery_voice_candidate as inspector
inspector.APPS['cleaner'] = ('com.universalrp.cleansweep', '2.90', 95, 'live-guard-v2.89', 'CleanSweep-v2.89.apk',
    '1eb064d81102c7cc32fb1cb9ea386b6cb3aa235900b1c119b1783b68f342c698', 'CleanSweep-v2.90.apk')
if __name__ == '__main__':
    inspector.main('cleaner')
