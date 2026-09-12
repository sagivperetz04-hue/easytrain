begin;

select plan(1);

select ok(true, 'pgTAP runs against the local stack');

select * from finish();

rollback;
