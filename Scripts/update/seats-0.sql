-- %%
-- schema: gros
-- table: seats
-- action: create
-- %%

CREATE TABLE "gros"."seats" (
	"project_id" INTEGER NOT NULL,
	"sprint_id" INTEGER NOT NULL,
	"date" TIMESTAMP NOT NULL,
	"seats" FLOAT NULL,
		CONSTRAINT "pk_seat_range_id" PRIMARY KEY ("project_id", "sprint_id", "date")
);
