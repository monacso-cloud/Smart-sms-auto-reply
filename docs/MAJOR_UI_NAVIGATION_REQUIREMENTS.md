# ReplyDesk: Business SMS Bot — Major UI / Navigation Requirement

This document is the locked UI/navigation requirement supplied by the owner.

## Core rule
ReplyDesk must be a multi-page business application. Do not put all features, settings, chatbot rules, logs and configuration fields on one long scrolling screen.

## Main dashboard
Show:
- ReplyDesk Bot ON/OFF
- Missed calls today
- SMS received today
- Bot replies today
- Unmatched questions
- Staff assistance requests
- Failed SMS
- Permission status

Main navigation:
1. Missed Call Auto Reply
2. SMS Auto Bot
3. Auto Reply Library
4. Staff Assistance
5. Business Hours
6. After-Hours Reply
7. Conversations
8. Activity & Call Logs
9. Test Bot
10. Message Templates
11. Contacts / Customers
12. Scheduled Messages
13. Business Profile
14. Bot Settings
15. Permissions & Diagnostics
16. Subscription / Account
17. Help & Support

Each opens its own full-screen page.

## Required full-screen sections
- Missed Call Auto Reply
- SMS Auto Bot
- Auto Reply Library
- Add / Edit Auto Reply
- Departments
- Unmatched Messages
- Staff Assistance
- Business Hours
- After-Hours Bot
- Conversations
- Activity & Call Logs
- Test Bot
- Message Templates
- Bot Safety & Behaviour
- Business Profile
- Permissions & Diagnostics
- Subscription / Account
- Help & Support

## Auto Reply Library
Must scale to hundreds or thousands of rules with search, filters, departments, categories and full-screen rule editing. Normal users must never edit technical keyword=>reply syntax.

## Staff handover
When staff assistance is detected:
- flag STAFF REQUIRED
- notify business
- pause bot for conversation
- allow manual staff reply
- bot must not interrupt
- manual or timed resume

## Business hours / after hours
Provide weekly hours, holiday mode, away mode, temporary closure, special hours, after-hours message, booking link, emergency/contact information and staff availability.

## Conversations / activity
Provide full-screen inbox and activity screens with tabs, search and filters. Do not embed logs in tiny boxes.

## Test Bot
Must simulate matching without sending a real SMS or incurring SMS charges.

## First-time setup
Wizard:
1. Business type
2. Business name
3. Business hours
4. Website / booking URL
5. Address
6. Starter Auto Reply categories
7. Missed-call reply
8. Test Bot

## Navigation
Bottom navigation:
- HOME
- CONVERSATIONS
- AUTO BOT
- ACTIVITY
- SETTINGS

AUTO BOT contains:
- Auto Reply Library
- Departments
- Unmatched Messages
- Staff Assistance
- Test Bot

SETTINGS contains:
- Business Profile
- Business Hours
- After Hours
- Reply Delay
- Notifications
- Permissions
- Bot Safety
- Subscription
- Help

## Design
Use full-screen pages, large touch targets, clear headings, search, categories, tabs, cards, back navigation, Save buttons, clear ON/OFF states and large editable text fields.

Avoid tiny text boxes, very long single screens, technical syntax exposed to users, small embedded logs and excessive scrolling.

## Scalability
Must support solo businesses, clinics, salons, trades, automotive, healthcare, aged care, retail, service businesses, multi-department companies and larger organisations.

Product name is locked as: **ReplyDesk: Business SMS Bot**
