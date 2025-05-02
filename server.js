const path=require("path");
const fastify=require("fastify")();
const fastifyStatic=require("@fastify/static");
const port=6006;
fastify.register(fastifyStatic,{
    root: path.join(__dirname),
    wildcardSuffix: "/index.html",
    onSend: (request, reply)=>{
        reply.header("Cache-Control", "no-cache, no-store, must-revalidate");
    }
});
fastify.get("/*", async (request, reply)=>{
    reply.header("Cache-Control", "no-cache, no-store, must-revalidate");
    return reply.code(404).type("text/html; charset=utf-8").send("<h1>404 Not Found</h1>");
});
fastify.setErrorHandler((error, request, reply)=>{
    reply.header("Cache-Control", "no-cache, no-store, must-revalidate");
    return reply.code(500).type("text/plain; charset=utf-8").send(`Server error: ${error.message}`);
});
const start=async ()=>{
    try{
        await fastify.listen({ port, host: "::" });
        console.log(`Server running at http://localhost:${port}`);
    }
    catch (err){
        fastify.log.error(err);
        process.exit(1);
    }
};
start();