


## Shortcomings
- Upper age limit is only checked in years (lower age limit doesn't matter because if user in anything below 18 years, they are underage)
- Upper age limit calculations are done with integers, because current maximum loan period is 60 months, which in years is exactly 5. If maximum loan period changes, calculation variables should be re-evaluated.
- Life expectancies data is unrealistic, especially for men (but had to be chosen so that segemnt3 personal code test would pass)