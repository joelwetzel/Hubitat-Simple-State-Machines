# Simple State Machines for Hubitat *** BETA ***

Create state machines in Hubitat!

*** This is a BETA ***

I have not yet written up detailed instructions on install.  This is a BETA, and should only be used by those already familiar with custom apps and drivers in Hubitat.

There are two custom apps and two drivers to install.

The best way to learn how to use this is to watch a Youtube video I made of it.  The first 5 minutes are explaining the concepts.  Skip to 5:03 if you just want to see the demo in action.  

But until I write up full documentation, I will paste in the discussion that led me to create it:

----------------

FYI.  I read your comment about state machines back when you made it, and it intrigued me.  So for the last week, I've been working on an app for defining state machines in HE.

I'm a software engineer by trade, and I love state machines for certain applications.  (Also the old saying about any class eventually becoming a state machine.  The distinction is if you know that it's a state machine and plan it out, or you just allow states and transitions at random.  The quote at the top of this article is excellent:  http://raganwald.com/2018/02/23/forde.html)

So when I see people posting screenshots of their RM rules with multiple IF - ELSEIF - ELSE blocks with different conditions in each and they wonder why it doesn't catch an edge case, my engineer brain says "That's a place for a state machine!"

However, I'm torn over whether to release it or not.  Even RM4 is edging into the point of being powerful enough for non-programmers to get themselves into a lot of trouble, as evidenced by those posts.  It has the power to do recursive, iterative conditional logic.  For users who aren't used to that kind of power, they don't realize the pitfalls.  For professional engineers, we see certain patterns in code and think "code smell!" and then refactor and clean it up.

So for a professional, you see "smelly" code that can be simplified and more reliable by using a state machine.  But the tradeoff is an additional level of abstraction.  And for typical users, the abstraction of a well-defined state machine is not something they're used to seeing.

However, the most likely result is I release the app and no one uses it because it's too confusing.  So I probably will release it when I feel it's ready.

One thing that would help me:  what use cases would you want to use a state machine for?  This is the first thing I've built for Hubitat that didn't come from one of my own needs.  I've tried to keep my automations simple, and haven't needed a state machine yet.  I'm just building it because it's cool.

To give an idea of the "spec" of my app:
- ability to define "states"
  - a state is a child device that implements the "switch" capability, so you can use RM to perform actions either when you enter or leave a state.
- ability to define "events"
  - an event is a child device that implements the "button" capability, so you can use any arbitrary RM rule or other app to trigger the event
- ability to define "transitions"
  - a transition means that if you are in the "from" state, and the chosen event fires, the state machine should transition to the "to" state.  (the from state turns off, and the to state turns on.)
- don't re-implement RM.  Rely on RM to perform actions from the state transitions.  Rely on RM to trigger the events.  Just be the core of the state machine that manages the state and transitions.
